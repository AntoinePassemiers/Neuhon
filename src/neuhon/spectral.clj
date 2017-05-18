(ns neuhon.spectral
  ^{:doc "Least-squares spectral analysis"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(deftype PreprocessedWaveArrays
  [^Double  freq
   ^Double  tau
   ^doubles waveform-cos
   ^doubles waveform-sin
   ^Double  den-cos
   ^Double  den-sin])

;; Number of different keys to consider
(def ^:const keyboard-size
  (- max-midi-note min-midi-note))

;; Frequency bins between min-midi-node and max-midi-node
(def ^:const note-frequencies
  (mapv midi-to-hertz (range (- min-midi-note 1) (- max-midi-note 1))))

;; Periods corresponding to the frequency bins
(def ^:const note-periods
  (mapv #(int (Math/round (/ target-sampling-rate %1))) note-frequencies))

;; Constant for converting from frequency to pulse rate
(def ^:const pulse-conversion-factor (* 2.0 (/ Math/PI target-sampling-rate)))

;; Time line for generating sine waves
(def ^:dynamic timeline (range window-size))

(defn vsin
  "Vectorized sinus function"
  [^clojure.lang.ISeq coll]
  (mapv #(Math/sin %1) coll))

(defn vcos
  "Vectorized cosinus function"
  [^clojure.lang.ISeq coll]
  (mapv #(Math/cos %1) coll))

(defn freq-time-delay
  "Computes the phase change of a given frequency,
  based on the Lomb-Scargle method."
  [^Double freq]
  (/
    (Math/atan
      (/
        (apply-sum 
          (vsin 
            (mapv #(* pulse-conversion-factor %1 freq) timeline)))
        (apply-sum 
          (vcos 
            (mapv #(* pulse-conversion-factor %1 freq) timeline)))))
    (* pulse-conversion-factor freq)))

(defn arr-vproduct
  "Elemwise product of two arrays"
  [^doubles A ^doubles B]
  (let [conv #(* (aget A %1) (aget B %1))]
    (reduce
      #(+ (conv %1) (conv %2))
      (range (alength A)))))

(defn cosine-waveform
  "Generates a cosine wave with given phase change (as Java array)"
  [^Double freq ^Double tau]
  (let [arr (make-array Double/TYPE (count timeline))]
    (do
      (mapv
        #(aset-double arr %1
          (Math/cos 
            (* pulse-conversion-factor (- %1 tau) freq)))
        timeline)
      arr)))

(defn sine-waveform
  "Generates a sine wave with given phase change (as Java array)"
  [^Double freq ^Double tau]
  (let [arr (make-array Double/TYPE (count timeline))]
    (do
      (mapv
        #(aset-double arr %1
          (Math/sin 
            (* pulse-conversion-factor (- %1 tau) freq)))
        timeline)
      arr)))

(defn lomb-scargle-preprocessing
  "Pre-computes what can be preprocessed during the
  Lomb-Scargle least-squares regression"
  []
  (for [i (range keyboard-size)]
    (let [^Double freq (nth note-frequencies i)
          ^Double tau (freq-time-delay freq)
          ^doubles cos-wave (cosine-waveform freq tau)
          ^doubles sin-wave (sine-waveform freq tau)]
      (PreprocessedWaveArrays. freq tau cos-wave sin-wave
        (apply-sum (mapv #(pow2 %1) cos-wave))
        (apply-sum (mapv #(pow2 %1) sin-wave))))))

(def ^clojure.lang.LazySeq ls-freqs (doall (lomb-scargle-preprocessing)))

(defn apply-lomb-scargle-on-one-frequency
  "Evaluates the Lomb-Scargle periodogram at a given frequency,
  where freq-data is of PreprocessedWaveArrays type"
  [^doubles signal ^PreprocessedWaveArrays freq-data]
  (* 0.5 (+
    (/ 
      (pow2 
        (arr-vproduct (.waveform-cos freq-data) signal))
      (.den-cos freq-data))
    (/ 
      (pow2 
        (arr-vproduct (.waveform-sin freq-data) signal))
      (.den-sin freq-data)))))

(defn compute-periodogram
  "Computes the Lomb-Scargle periodogram, given an input sequence"
  [signal]
  (mapv 
    #(apply-lomb-scargle-on-one-frequency signal %1)
    ls-freqs))

;; (doall (compute-periodogram (into-array Double/TYPE (range 4096))))