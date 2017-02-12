(defn log_2 [n] (/ (Math/log n) (Math/log 2)))

;; http://asymmetrical-view.com/2009/07/02/clojure-primitive-arrays.html
;; https://equilibriumofnothing.wordpress.com/2013/10/14/algorithm-iterative-fft/

(set! *warn-on-reflection* false)
(set! *unchecked-math* true)

;; Wav.clj : mean of the two channels / add assertions
;; iterative fft
;; low pass filter
;; Sha'ath profiles
;; 

(def data-type Double/TYPE)

(deftype ComplexSeq [real imag length])
(deftype ComplexArray [^doubles real ^doubles imag])
(deftype Complex [^doubles real ^doubles imag])

(defn new-complex-array 
  ([N] 
    (ComplexArray. (make-array data-type N) (make-array data-type N)))
  ([^doubles signal N] 
    (ComplexArray. (into-array data-type signal) (make-array data-type N))))

(defn complex-to-real [arr]
  (let [N (alength (.real arr))
        new_arr (make-array data-type N)]
    (do 
      (loop [i 0]
      (when (< i N)
        (aset new_arr i (Math/sqrt 
          (+ (Math/pow (aget (.real arr) i) 2) 
            (Math/pow (aget (.imag arr) i) 2))))
        (recur (inc i))))
      new_arr)))

(defn get-complex [arr k]
  (Complex. (aget (.real arr) k) (aget (.imag arr) k)))

(defn set-complex 
  ([arr k source kk]
    (do
      (println k (alength (.real arr)))
      (aset (.real arr) k (double (nth (.real source) kk)))
      (aset (.imag arr) k (double (nth (.imag source) kk)))))
  ([arr k c] 
    (do 
      (aset (.real arr) k (.real c))
      (aset (.imag arr) k (.imag c)))))

(defn complex-plus [c c2]
  (let [new_real (+ (.real c) (.real c2))
        new_imag (+ (.imag c2) (.imag c2))]
    (Complex. new_real new_imag)))

(defn complex-minus [c c2]
  (let [new_real (- (.real c) (.real c2))
        new_imag (- (.imag c2) (.imag c2))]
    (Complex. new_real new_imag)))

(defn apply-complex-mult [c c2]
  (let [a_real   (.real c)
        a_imag   (.imag c)
        b_real   (.real c2)
        b_imag   (.imag c2)
        new_real (- (* a_real b_real) (* a_imag b_imag))
        new_imag (+ (* a_real b_imag) (* a_imag b_real))]
    (Complex. new_real new_imag)))

(defn complex-from-angle [angle] 
  (Complex. (Math/cos angle) (Math/sin angle)))

(defn seq-drop [n complex-seq]
  (let [real-part (drop n (.real complex-seq))
        imag-part (drop n (.imag complex-seq))]
    (ComplexSeq. real-part imag-part (- (.length complex-seq) n))))

(defn new-complex-seq [signal] 
  (let [N (count signal)]
    (ComplexSeq. signal (replicate N 0) N)))

(defn complex-seq-to-real [data]
  (let [N (count (.real data))
        real-part (.real data)
        imag-part (.imag data)]
    (map 
      (fn [i]
        (Math/sqrt (+
          (Math/pow (nth real-part i) 2)
          (Math/pow (nth imag-part i) 2))))
      (range N))))

(defn seq-FFT 
  ([signal] (seq-FFT signal (count signal) 1))
  ([signal N] (seq-FFT signal N 1))
  ([signal N s]
    (let [half      (/ N 2)
          in_real   (.real signal)
          in_imag   (.imag signal)]
      (if (= N 1)
        (ComplexArray. 
          (to-array [(first in_real)])
          (to-array [(first in_imag)]))
        (let [q (seq-FFT signal half (* 2 s))
              r (seq-FFT (seq-drop s signal) half (* 2 s))
              result (new-complex-array N)]
          (do
            (loop [i 0]
              (when (< i half) (do
                (let [wk (complex-from-angle (- (/ (* (* 2 Math/PI) i) N)))
                      rk (apply-complex-mult wk (get-complex r i))
                      ya (complex-plus (get-complex q i) rk)
                      yb (complex-minus (get-complex q i) rk)]
                  (do 
                    (set-complex result i ya)
                    (set-complex result (+ i half) yb))))
                    (recur (inc i))))
            result))))))


(defn reverse-bits [k]
  (let [k-1sp (bit-or (bit-shift-left (bit-and k 0x55555555) 1) 
          (bit-and (bit-shift-right k 1) 0x55555555))
        k-2sp (bit-or (bit-shift-left (bit-and k-1sp 0x33333333) 2) 
          (bit-and (bit-shift-right k-1sp 2) 0x33333333))
        k-4sp (bit-or (bit-shift-left (bit-and k-2sp 0x0f0f0f0f) 4) 
          (bit-and (bit-shift-right k-2sp 4) 0x0f0f0f0f))
        k-8sp (bit-or (bit-shift-left (bit-and k-4sp 0x00ff00ff) 8) 
          (bit-and (bit-shift-right k-4sp 8) 0x00ff00ff))
        k-16sp (bit-or (bit-shift-left k-8sp 16) 
          (bit-shift-right k-8sp 16) 0x00ff00ff)]
    k-16sp))

(defn iterative-FFT [signal]
  (let [N (.length signal)
        nbits (log_2 N)
        toinverse (< nbits 0)
        abs-nbits (Math/abs nbits)
        result (new-complex-array N)
        omega (atom 1)]
    (do
      (loop [i 0]
        (when (< i N)
          (set-complex result i signal
            (bit-shift-right (int (reverse-bits (int i))) (int (- 32 abs-nbits))))
          (recur (inc i))))
      (println "End of first loop !")
      (loop [p 1]
        (when (<= p abs-nbits)
          (let [step (bit-shift-left 0x1 p)
                theta (/ (* 2 (* Math/PI (if toinverse -1 1))) step)
                wk (complex-from-angle theta)]
            (loop [offset 0]
              (when (< offset N)
                () ;; TODO
                (recur (offset + step)))))
          (recur (inc p))))
      result)))