(ns neuhon.spectral
  ^{:doc "Least-squares spectral analysis"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))


(deftype PreprocessedWaveforms
  [^Double               freq
   ^Double               tau
   ^clojure.lang.LazySeq waveform-cos
   ^clojure.lang.LazySeq waveform-sin
   ^Double               den-cos
   ^Double               den-sin])

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

(defn vproduct
  "Vectorized product between two sequences"
  [^clojure.lang.ISeq A ^clojure.lang.ISeq B]
  (mapv #(* %1 %2) A B))

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

(defn cos-waveform
  "Generates a cos-waveform with given phase change"
  [^Double freq ^Double tau]
  (mapv
    #(Math/cos 
      (* pulse-conversion-factor (- %1 tau) freq))
    timeline))

(defn sin-waveform
  "Generates a sin-waveform with given phase change"
  [^Double freq ^Double tau]
  (mapv
    #(Math/sin
      (* pulse-conversion-factor (- %1 tau) freq))
    timeline))

(defn lomb-scargle-preprocessing
  "Pre-computes what can be preprocessed during the
  Lonm-Scargle least-squares regression"
  []
  (for [i (range keyboard-size)]
    (let [freq (nth note-frequencies i)
          tau (freq-time-delay freq)
          ^clojure.lang.LazySeq cos-wave (cos-waveform freq tau)
          ^clojure.lang.LazySeq sin-wave (sin-waveform freq tau)]
      (PreprocessedWaveforms.
        freq
        tau
        cos-wave
        sin-wave
        (apply-sum (mapv #(pow2 %1) cos-wave))
        (apply-sum (mapv #(pow2 %1) sin-wave))))))

(def ls-freqs (doall (lomb-scargle-preprocessing)))

(defn apply-lomb-scargle-on-one-frequency
  "Evaluates the Lomb-Scargle periodogram at a given frequency,
  where freq-data is of PreprocessedWaveforms type"
  [^clojure.lang.ISeq signal ^PreprocessedWaveforms freq-data]
  (* 0.5 (+
    (/ (pow2 
      (apply-sum 
        (vproduct (.waveform-cos freq-data) signal))) (.den-cos freq-data))
    (/ (pow2 
      (apply-sum 
        (vproduct (.waveform-sin freq-data) signal))) (.den-sin freq-data)))))

(defn compute-periodogram
  "Computes the Lomb-Scargle periodogram, given an input sequence"
  [^clojure.lang.ISeq signal]
  (mapv 
    #(apply-lomb-scargle-on-one-frequency signal (nth ls-freqs %1))
    (range n-midi-notes)))