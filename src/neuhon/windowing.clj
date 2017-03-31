(ns neuhon.windowing
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

;; Lowest note considered in Hertz
(def lowest-midi-note-default (int 9))

;; Highest note considered in Hertz
(def highest-midi-note-default (int 81))

;; Resolution of the Fourier transform
(def spectrum-size-default (int 16384))

;; Framerate after resampling of the raw audio
(def sampling-freq-default (float 4410.0))

;; Arbitrary p parameter for determining the value of the constant Q
(def p-default (float 0.8))

;; Primitive type of the spectral coefficients
(def win-coef-type Double/TYPE)

(defn generic-blackman-window 
  "Returns a new function with fixed cosine coefficients.
  The new function will generate a Blackan window, based on these coefficients.
  Source : https://en.wikipedia.org/wiki/Window_function#Blackman_windows"
  [N a0 a1 a2 a3]
  (fn [n]
    (+ a0
      (- (* a1 (Math/cos (/ (* 2 Math/PI n) (- N 1))))
        (+ (* a2 (Math/cos (/ (* 4 Math/PI n) (- N 1))))
          (- (* a3 (Math/cos (/ (* 6 Math/PI n) (- N 1))))))))))

(defn blackman-window-func 
  "Computes one coefficient of the Blackman window function,
  based on the input index"
  [N]
  (generic-blackman-window N 0.42659 0.49656 0.076849 0.0))

(defn nuttall-window-func 
  "Computes one coefficient of the Nuttal window function,
  based on the input index"
  [N]
  (generic-blackman-window N 0.355768 0.487396 0.144232 0.012604))

(defn blackman-nuttall-window-func 
  "Computes one coefficient of the Blackman-Nuttal window function,
  based on the input index"
  [N]
  (generic-blackman-window N 0.3635819 0.4891775 0.1365995 0.0106411))

(defn blackman-harris-window-func 
  "Computes one coefficient of the Blackman-Harris window function,
  based on the input index"
  [N]
  (generic-blackman-window N 0.35875 0.48829 0.14128 0.01168))

(defn create-window
  "Computes all the coefficients of a given window function"
  [N window-func]
  (let [f (window-func N)]
    (map f (range N))))

(defn convolute
  "Applies the convolution between an input signal and a window"
  [signal window]
  (let [convolution (fn [i] (* (nth signal i) (nth window i)))]
    (map convolution (range (count window)))))

(comment "A spectral window type for storing the window coefficients,
  as well of the left and right bounds of the window")
(deftype Window [coefs lk rk])

(comment "An abstract type for storing all the windows required by the CQT algorithm,
  where each of the windows will be convoluted with the spectrum to compute
  one of the coefficients of the extended chromatic vector")
(deftype WindowMatrix [coefs lks rks]
  clojure.lang.IPersistentCollection
  (seq [self] (if (seq coefs) self nil))
  (cons [self o] (WindowMatrix. coefs (conj lks o) (conj rks o)))
  (empty [self] (WindowMatrix. [] [] []))
  (equiv
    [self o]
    (if (instance? WindowMatrix o)
      (and (= lks (.lks o))
           (= rks (.rks o)))
     false))
  clojure.lang.ISeq
  (first [self] (Window. (first coefs) (first lks) (first rks)))
  (next [self] (Window. (next coefs) (next lks) (next rks)))
  (more [self] (Window. (rest coefs) (rest lks) (rest rks)))
  Object
  (toString [self] 
    (str "WindowMatrix type")))

(defn win-nth 
  "Gets the spectral window corresponding to a given bin"
  [matrix n]
  (Window. (nth (.coefs matrix) n) (nth (.lks matrix) n) (nth (.rks matrix) n)))

(defn get-Q-from-p 
  "Computes the constant Q, based on an arbitrary parameter p"
  [p]
  (* p (- (Math/pow 2 (/ 1.0 12)) 1)))

(defn win-left-bound
  "Left bound index of the spectral window that corresponds to fk"
  [Q fk N sampling-rate]
  (double (Math/round (* (- 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn win-right-bound
  "Right bound index of the spectral window that corresponds to fk"
  [Q fk N sampling-rate] 
  (double (Math/round (* (+ 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn cosine-win-element 
  "Computes one coefficient of the cosine temporal window"
  [x lk rk]
  (let [relative-pos (/ (double (- x lk)) (- rk lk))]
    (- 1.0 (Math/cos (* 2 (double (* Math/PI relative-pos)))))))

(defn cosine-win
  "Computes all the coefficients of the cosine temporal window"
  [Q fk N sampling-rate]
  (let [lk (win-left-bound Q fk N sampling-rate)
        rk (win-right-bound Q fk N sampling-rate)
        win-size (- rk lk)
        win (make-array win-coef-type win-size)]
    (Window. 
      (map 
        (fn [i] (cosine-win-element (+ lk i) lk rk)) 
        (range 0 win-size)) lk rk)))

;; Pre-computes a sequence of temporal cosine windows with fixed size
(def cosine-windows 
  (doall 
    (map 
      (fn [d]
        (cosine-win 
          (get-Q-from-p p-default) 
          (midi-to-hertz d) spectrum-size-default sampling-freq-default))
      (range lowest-midi-note-default highest-midi-note-default))))

;; Converts a sequence of Windows into a WindowMatrix
(def winmat (WindowMatrix. 
  (doall (map (fn [win] (.coefs win)) cosine-windows))
  (doall (map (fn [win] (.lk win)) cosine-windows)) 
  (doall (map (fn [win] (.rk win)) cosine-windows))))

(defn apply-win-on-spectrum 
  "Computes a QCT coefficient by applying a spectral window,
  given the spectrum and a certain bin"
  [spectrum k]
  (let [win (win-nth winmat k)
        lk (.lk win)
        rk (.rk win)
        coefs (.coefs win)
        convolution (fn [i] (* (nth spectrum (+ lk i)) (nth coefs i)))]
    (apply-sum (map convolution (range (- rk lk))))))

(defn note-subset
  "Returns the values in an extended chromatic vector
  that match a given musical note"
  [chromatic-vector note-index]
  (doall (map (fn [i] (nth chromatic-vector (+ note-index i))) 
    (range 0 (- (count chromatic-vector) 12) 12))))

(defn note-score 
  "Scores how well a chromatic vector matches a given note"
  [chromatic-vector note-index]
  (let [subset (note-subset chromatic-vector note-index)]
    (+ (* 0.8 (apply max subset)) (* 0.2 (apply-sum subset)))))