(def lowest-midi-note-default (int 0))
(def highest-midi-note-default (int 97))
(def spectrum-size-default (int 2048))
(def sampling-freq-default (float 4410.0))

(defn midi-to-hertz [d]
  (* 440.0 (Math/pow 2 (float (/ (- d 69.0) 12.0)))))

(def win-coef-type Double/TYPE)

(defn generic-blackman-window [N a0 a1 a2 a3]
  (fn [n]
    (+ a0
      (- (* a1 (Math/cos (/ (* 2 Math/PI n) (- N 1))))
        (+ (* a2 (Math/cos (/ (* 4 Math/PI n) (- N 1))))
          (- (* a3 (Math/cos (/ (* 6 Math/PI n) (- N 1))))))))))

(defn blackman-window-func [N]
  (blackman-window N 0.42659 0.49656 0.076849 0.0))

(defn nuttall-window-func [N]
  (blackman-window N 0.355768 0.487396 0.144232 0.012604))

(defn blackman-nuttall-window-func [N]
  (blackman-window N 0.3635819 0.4891775 0.1365995 0.0106411))

(defn blackman-harris-window-func [N]
  (blackman-window N 0.35875 0.48829 0.14128 0.01168))

(defn create-window [N window-func]
  (let [f (window-func N)]
    (map f (range N))))

(defn convolute [signal window]
  (let [convolution (fn [i] (* (nth signal i) (nth window i)))]
    (map convolution (range (count window)))))

(deftype Window [coefs lk rk])

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

(defn win-nth [matrix n]
  (Window. (nth (.coefs matrix) n) (nth (.lks matrix) n) (nth (.rks matrix) n)))

(defn get-Q-from-p [p]
  (* p (- (long (Math/pow 2 (/ 1.0 12))) 1)))

(defn win-left-bound [Q fk N sampling-rate] 
  (double (Math/round (* (- 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn win-right-bound [Q fk N sampling-rate] 
  (double (Math/round (* (+ 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn cosine-win-element [x lk rk]
  (let [relative-pos (/ (double (- x lk)) (- rk lk))]
    (- 1.0 (Math/cos (* 2 (double (* Math/PI relative-pos)))))))

(defn cosine-win [Q fk N sampling-rate]
  (let [lk (win-left-bound Q fk N sampling-rate)
        rk (win-right-bound Q fk N sampling-rate)
        win-size (- rk lk)
        win (make-array win-coef-type win-size)]
    (Window. (map (fn [i] (cosine-win-element (+ lk i) lk rk)) (range 0 win-size)) lk rk)))

(def cosine-windows (doall (map 
  (fn [d] 
    (cosine-win 0.1 (midi-to-hertz d) spectrum-size-default sampling-freq-default))
    (range lowest-midi-note-default highest-midi-note-default))))

(def winmat (WindowMatrix. 
  (doall (map (fn [win] (.coefs win)) cosine-windows))
  (doall (map (fn [win] (.lk win)) cosine-windows)) 
  (doall (map (fn [win] (.rk win)) cosine-windows))))

(defn apply-win-on-spectrum [spectrum k]
  (let [win (win-nth winmat k)
        lk (.lk win)
        rk (.rk win)
        coefs (.coefs win)
        convolution (fn [i] (* (nth spectrum (+ lk i)) (nth coefs i)))]
    (reduce + (map convolution (range (- rk lk))))))

(defn arg-max 
  ([data] (arg-max data 0 Double/MIN_VALUE 0))
  ([data begin end] (arg-max data begin Double/MIN_VALUE 0))
  ([data begin max-value best-index] 
  (do (if (= (count data) begin)
    best-index
    (if (> (nth data begin) max-value)
      (arg-max data (+ begin 1) (nth data begin) begin)
      (arg-max data (+ begin 1) max-value best-index))))))

(defn sum [data]
  (reduce + data))

(defn note-subset [chromatic-vector note-index]
  (doall (map (fn [i] (nth chromatic-vector (+ note-index i))) 
    (range 0 (- (count chromatic-vector) 12) 12))))

(defn note-score [chromatic-vector note-index]
  (let [subset (note-subset chromatic-vector note-index)]
    (+ (* 0.8 (apply max subset)) (* 0.2 (sum subset)))))