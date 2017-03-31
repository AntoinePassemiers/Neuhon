(ns neuhon.spectral
  ^{:doc "Least-squares spectral analysis"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))


(deftype PreprocessedWaveforms
  [freq
   tau
   waveform-cos
   waveform-sin
   den-cos
   den-sin])

;; Number of different keys to consider
(def ^:const keyboard-size
  (- max-midi-note min-midi-note))

;; Frequency bins between min-midi-node and max-midi-node
(def ^:const note-frequencies
  (map midi-to-hertz (range min-midi-note max-midi-note)))

;; Periods corresponding to the frequency bins
(def ^:const note-periods
  (map #(int (Math/round (/ target-sampling-rate %1))) note-frequencies))

;; Constant for converting from frequency to pulse rate
(def ^:const pulse-conversion-factor (* 2.0 (/ Math/PI target-sampling-rate)))

(defn vsin
  "Vectorized sinus function"
  [coll]
  (map #(Math/sin %1) coll))

(defn vcos
  "Vectorized cosinus function"
  [coll]
  (map #(Math/cos %1) coll))

(defn vproduct
  "Vectorized product between two sequences"
  [A B]
  (map #(* %1 %2) A B))

(def timeline (range window-size))

(defn freq-time-delay
  "Computes the phase change of a given frequency,
  based on the Lomb-Scargle method."
  [freq]
  (/
    (Math/atan
      (/
        (apply-sum 
          (vsin 
            (map #(* pulse-conversion-factor %1 freq) timeline)))
        (apply-sum 
          (vcos 
            (map #(* pulse-conversion-factor %1 freq) timeline)))))
    (* pulse-conversion-factor freq)))

(defn cos-waveform
  "Generates a cos-waveform with given phase change"
  [freq tau]
  (map
    #(Math/cos 
      (* pulse-conversion-factor (- %1 tau) freq))
    timeline))

(defn sin-waveform
  "Generates a sin-waveform with given phase change"
  [freq tau]
  (map
    #(Math/sin
      (* pulse-conversion-factor (- %1 tau) freq))
    timeline))

(defn corr-between-two-colls
  "Correlation function between two collections"
  [A B]
  (apply-sum (vproduct A B)))

(defn lomb-scargle-preprocessing
  "Pre-computes what can be preprocessed during the
  Lonm-Scargle least-squares regression"
  []
  (for [i (range keyboard-size)]
    (let [freq (nth note-frequencies i)
          tau (freq-time-delay freq)
          cos-wave (cos-waveform freq tau)
          sin-wave (sin-waveform freq tau)]
      (PreprocessedWaveforms.
        freq
        tau
        cos-wave
        sin-wave
        (apply-sum (map #(pow2 %1) cos-wave))
        (apply-sum (map #(pow2 %1) sin-wave))))))

(def s (cos-waveform 440.0 (freq-time-delay 440.0)))

(def stuff (lomb-scargle-preprocessing))
stuff