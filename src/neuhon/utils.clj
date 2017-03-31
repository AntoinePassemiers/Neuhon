(ns neuhon.utils
  ^{:author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.core.matrix]))


;; Number of channels in the wav files
(def ^:const n-channels 2)

;; Sampling rate after the final resampling
(def ^:const target-sampling-rate 4410.0)

;; Sampling rate before re-sampling
(def ^:const sampling-rate 44100.0)

;; Length of the sliding window
(def ^:const window-size 16384)

;; Lowest midi note to consider
(def ^:const min-midi-note 28)

;; Highest midi note to consider
(def ^:const max-midi-note 88)

;; Number of octaves := (max-midi-node - min-midi-node) / 12
(def ^:const n-octaves 5)
(assert (= (/ (- max-midi-note min-midi-note) 12) n-octaves))

;; Proportion between highest amplitude and mean amplitude to build chromatic vectors
(def ^:const chromatic_max_weight 0.8)


(defn apply-sum
  "Sums the elements of an input sequence"
  [data]
  (reduce + data))

(defn log_2 
  "Applies a binary logarithm on an input number"
  [n] 
  (/ (Math/log n) (Math/log 2)))

(defn midi-to-hertz
  "Midi note to frequency conversion"
  [note]
  (* 440.0 (Math/pow 2.0 (/ (- note 69.0) 12.0))))

(defn hertz-to-midi
  "Frequency to midi note conversion"
  [freq]
  (+ 69.0 (* 12.0 (log_2 (/ freq 440.0)))))

(defn pow2
  "Power of two of an input number"
  [value]
  (* value value))

(defn ste
  "Short term energy of an input signal, starting at start
  and ending at start + length"
  [data start length]
  (do
    (reduce + 
      (map
        (fn [i] (pow2 (nth data i)))
        (range start (+ start length))))))

(defn inc-array-element
  "Increment an array at a given index"
  [arr i]
  (aset arr i (+ 1 (aget arr i))))

(defn convert-to-array
  "Converts an input sequence to a Java array"
  [sequence start length]
  (let [new_array (make-array Double/TYPE length)]
    (doall
      (map
        (fn [i] (aset new_array i (double (nth sequence (+ start i)))))
        (range length)))
    new_array))

(defn convert-to-complex-array
  "Converts an input sequence to a double-sized Java array"
  [sequence start length]
  (let [new_array (make-array Double/TYPE (* 2 length))]
    (doall
      (map
        (fn [i] (aset new_array i (double (nth sequence (+ start i)))))
        (range length)))
    new_array))