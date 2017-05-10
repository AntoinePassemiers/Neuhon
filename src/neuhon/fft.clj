(ns neuhon.fft
  ^{:doc "Fast Fourier Transform"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

;; Complex number type
(deftype Complex [^double real ^double imag])

(defn complex-plus
  "Sum two complex numbers"
  [^Complex c ^Complex c2]
  (Complex. 
    (+ (.real c) (.real c2))
    (+ (.imag c) (.imag c2))))

(defn complex-minus 
  "Subtract two complex numbers"
  [^Complex c ^Complex c2]
  (Complex.
    (- (.real c) (.real c2))
    (- (.imag c) (.imag c2))))

(defn complex-multiply
  "Multiply two complex numbers"
  [^Complex c ^Complex c2]
  (let [a_real   (.real c)
        a_imag   (.imag c)
        b_real   (.real c2)
        b_imag   (.imag c2)]
    (Complex.
      (- (* a_real b_real) (* a_imag b_imag))
      (+ (* a_real b_imag) (* a_imag b_real)))))

(defn to-complex-array
  "Creates an array of complex numbers"
  [sequence]
  (let [n (* 2 (count sequence))
        arr (make-array Double/TYPE n)]
    (do
      (mapv
        #(aset-double arr %1 %2)
        (range 0 n 2)
        sequence)
      arr)))

(defn aget-complex
  "Get complex value in an array at a given index"
  [^doubles arr ^Long i]
  (let [j (bit-shift-left i 1)]
    (Complex.
      (aget arr j)
      (aget arr (+ j 1)))))

(defn aset-complex
  "Set complex value in an array at a given index"
  [^doubles arr ^Long i ^Complex c]
  (let [j (bit-shift-left i 1)]
    (do
      (aset-double arr j (.real c))
      (aset-double arr (+ j 1) (.imag c)))))

(defn reverse-bits
  "Reverse the bits of an array index"
  [^Long i]
  (apply-sum
    (mapv
      #(bit-shift-left 
        (unsigned-bit-shift-right 
          (bit-and (bit-shift-left 1 %1) i) %1)
        (- (- spectral-space-n-bits 1) %1))
      (range spectral-space-n-bits))))

(defn bit-reverse-copy
  "Copy an array into a new array with permuted indices (bit reversal)"
  [^doubles src ^doubles dest]
  (mapv
    #(let [i (* 2 %1)
           j (* 2 (reverse-bits %1))]
      (do
        (aset-double dest j (aget src i))
        (aset-double dest (+ j 1) (aget src (+ i 1)))))
    (range (/ (alength src) 2))))

(defn divide-and-conquer
  "Subdivision of the problem of the Radix-2 FFT"
  [^doubles output ^Long s]
  (let [n (/ (alength output) 2)
        m (bit-shift-left 2 (- s 1))
        half-m (/ m 2)
        theta (- (/ (* 2 Math/PI) (double m)))
        omega-m (Complex. (Math/cos theta) (Math/sin theta))]
    (mapv
      (fn [k]
        (reduce
          (fn [omega j]
            (let [kj (+ k j)
                  t (complex-multiply omega 
                      (aget-complex output (+ kj half-m)))
                  u (aget-complex output kj)]
              (do
                (aset-complex output kj (complex-plus u t))
                (aset-complex output (+ kj half-m) (complex-minus u t))
                (complex-multiply omega omega-m))))
          (conj (range 0 (- (/ m 2) 1)) (Complex. 1 0))))
      (range 0 (- n 1) m))))

(defn iterative-radix2-fft
  "Iterative implementation of the Fast Fourier Transform"
  [^doubles input]
  (let [n (/ (alength input) 2)
        ^doubles output (make-array Double/TYPE (* 2 n))]
  (do
    (assert (= n window-size))
    (bit-reverse-copy input output)
    (mapv
      #(divide-and-conquer output %1)
      (range 1 (int (log_2 n))))
    output)))