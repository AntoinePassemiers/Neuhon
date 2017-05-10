(ns neuhon.windowing
  ^{:doc "Spectral and temporal windows"
  :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [neuhon.utils]))


;; Primitive type of the spectral coefficients
(def win-coef-type Double/TYPE)

(defn- generic-blackman-window 
  "Returns a new function with fixed cosine coefficients.
  The new function will generate a Blackan window, based on these coefficients.
  Source : https://en.wikipedia.org/wiki/Window_function#Blackman_windows"
  [N a0 a1 a2 a3]
  (fn [n]
    (- a0
      (- (* a1 (Math/cos (/ (* 2 Math/PI n) (- N 1))))
        (- (* a2 (Math/cos (/ (* 4 Math/PI n) (- N 1))))
          (* a3 (Math/cos (/ (* 6 Math/PI n) (- N 1)))))))))

(defn blackman-window-func 
  "Computes one coefficient of the Blackman window function,
  based on the input index"
  [N]
  (generic-blackman-window N 0.42 0.5 0.08 0.0))

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
    (map #(* %1 %2) signal window))

(comment "A spectral window type for storing the window coefficients,
  as well of the left and right bounds of the window")
(deftype Window [coefs li ri])

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
  (Window. 
    (nth (.coefs matrix) n) 
    (nth (.lks matrix) n) 
    (nth (.rks matrix) n)))

(defn get-Q-from-p 
  "Computes the constant Q, based on an arbitrary parameter p"
  [p]
  (* p (- (Math/pow 2 (/ 1.0 12)) 1)))

(defn win-left-bound
  "Left bound index of the spectral window that corresponds to fk"
  [Q fk N sampling-rate]
  (* 
    (- 1 (/ (double Q) 2)) 
    (/ 
      (double (* fk N)) 
      (double sampling-rate))))

(defn win-right-bound
  "Right bound index of the spectral window that corresponds to fk"
  [Q fk N sampling-rate] 
  (* 
    (+ 1 (/ (double Q) 2)) 
    (/ 
      (double (* fk N)) 
      (double sampling-rate))))

(defn nearest-bin-index
  "Index of the closest frequency found in the default frequency bins"
  [freq]
  (arg-min
    (mapv
      #(Math/abs (- %1 freq))
      frequency-bins)))

(defn cosine-win-element 
  "Computes one coefficient of the cosine temporal window"
  [x lk rk]
  (let [relative-pos (/ (double (- x lk)) (- rk lk))]
    (- 1.0 (Math/cos (* 2.0 (double (* Math/PI relative-pos)))))))

(defn norm-window
  "Divide the coefficients of a spectral window by their sum"
  [coefs]
  (mapv #(/ %1 (apply-sum coefs)) coefs))

(defn cosine-win
  "Computes all the coefficients of the cosine temporal window"
  [Q fk N sampling-rate]
  (let [lk (win-left-bound Q fk N sampling-rate)
        rk (win-right-bound Q fk N sampling-rate)
        li (nearest-bin-index lk)
        ri (nearest-bin-index rk)
        win-size (- (+ ri 1) li)]
    (Window. 
      (norm-window
        (map 
          (fn [i] (cosine-win-element (+ li i) lk rk)) 
          (range 0 win-size)))
      li 
      ri)))

;; Pre-computes a sequence of temporal cosine windows with fixed size
(def cosine-windows 
  (doall 
    (map 
      (fn [d]
        (cosine-win 
          Q-constant 
          (midi-to-hertz d) window-size target-sampling-rate))
      (range min-midi-note max-midi-note))))

;; Converts a sequence of Windows into a WindowMatrix
(def winmat (WindowMatrix. 
  (doall (map (fn [win] (.coefs win)) cosine-windows))
  (doall (map (fn [win] (.li win)) cosine-windows)) 
  (doall (map (fn [win] (.ri win)) cosine-windows))))

(defn apply-win-on-spectrum 
  "Computes a QCT coefficient by applying a spectral window,
  given the spectrum and a certain bin"
  [spectrum k]
  (let [win (win-nth winmat k)
        li (.li win)
        ri (.ri win)
        coefs (.coefs win)
        convolution (fn [i] (* (nth spectrum (+ li i)) (nth coefs i)))]
    (if
      (< 0 (count coefs))
      (apply-sum (map convolution (range (- (+ ri 1) li))))
      0.0)))

(defn dsk-transform
  "Direct spectral kernel transform of an input signal,
  given its Fourier transform"
  [spectrum]
  (doall
    (map
      (fn [i] (apply-win-on-spectrum spectrum i))
      (range (count cosine-windows)))))