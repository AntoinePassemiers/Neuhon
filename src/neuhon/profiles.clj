(ns neuhon.profiles
  ^{:doc "Major and minor scale profiles"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.stats]
        [neuhon.utils]))

;; Krumhansl's average profile of the major scale
(def krumhansl-major-base-profile (normalize [6.4 2.2 3.5 2.3 4.4 4.1 2.5 5.2 2.4 3.7 2.3 2.9]))

;; Krumhansl's average profile of the minor scale
(def krumhansl-minor-base-profile (normalize [6.4 2.8 3.6 5.4 2.7 3.6 2.6 4.8 4.0 2.7 3.3 3.2]))

;; Sha'ath's average profile of the major scale
(def shaath-major-base-profile (normalize [6.6 2.0 3.5 2.2 4.6 4.0 2.5 5.2 2.4 3.8 2.3 3.4]))

;; Sha'ath's average profile of the minor scale
(def shaath-minor-base-profile (normalize [6.5 2.8 3.5 5.4 2.7 3.5 2.5 5.1 4.0 2.7 4.3 3.2]))

;; Custom profile for the major scale
(def custom-major-base-profile (normalize [6.5 4.8 3.5 6.4 2.7 3.5 2.5 5.1 4.0 2.7 4.3 4.2]))

;; Custom profile for the minor scale
(def custom-minor-base-profile (normalize [6.6 4.0 3.5 4.2 4.6 4.0 2.5 5.2 2.4 3.8 2.3 4.4]))

(def major-base-profile custom-major-base-profile)
(def minor-base-profile custom-minor-base-profile)


;; Key signature names 
(def key-names ["C" "C#" "D" "Eb" "E" "F" "F#" "G" "G#" "A" "Bb" "B"])

;; Mapping the key signature names to fixed constants, expressed in semi-tones
(def key-values 
  (hash-map "C" 0 "C#" 1 "D" 2 "Eb" 3 "E" 4 "F" 5 "F#" 6 "G" 7 "G#" 8 "A" 9 "Bb" 10 "B" 11
    "Cm" 0 "C#m" 1 "Dm" 2 "Ebm" 3 "Em" 4 "Fm" 5 "F#m" 6 "Gm" 7 "G#m" 8 "Am" 9 "Bbm" 10 "Bm" 11))

;; Mapping the key signature names to 0 or 1
;; 0 corresponds to a major scale
;; 1 corresponds to a minor scale
(def key-types
  (hash-map "C" 0 "C#" 0 "D" 0 "Eb" 0 "E" 0 "F" 0 "F#" 0 "G" 0 "G#" 0 "A" 0 "Bb" 0 "B" 0
    "Cm" 1 "C#m" 1 "Dm" 1 "Ebm" 1 "Em" 1 "Fm" 1 "F#m" 1 "Gm" 1 "G#m" 1 "Am" 1 "Bbm" 1 "Bm" 1))

(defn rotate-left
  "Rotates the elements of a sequence to the left"
  ([profile] (rotate-left profile 1))
  ([profile n]
    (let [m (mod n 12)]
      (if (> m 0) 
        (rotate-left (concat (drop 1 profile) [(first profile)]) (- m 1))
        profile))))

(defn rotate-right
  "Rotates the elements of a sequence to the right"
  ([profile] (rotate-right profile 1))
  ([profile n]
    (let [m (mod n 12)]
      (if (> m 0) 
        (rotate-right (concat [(last profile)] (drop-last 1 profile)) (- m 1))
        profile))))

(defn key-distance 
  "Computes the distance between two key signatures,
  expressed in semi-tones"
  [key-a key-b]
  (let [value-a (get key-values key-a)
        value-b (get key-values key-b)]
    (mod (+ 12 (- value-a value-b)) 12)))

(defn key-to-str
  "Decodes an integer to a key signature.
  Minor keys are shifted 12 semi-tones
  away from the major ones. Examples :
    0   -->   C
    7   -->   F#
    11  -->   B
    12  -->   Cm
    16  -->   Em"
  [key]
  (if (< key 12)
    (nth key-names key)
    (str (nth key-names (- key 12)) "m")))

(defn is-same-key? 
  "Tells whether two keys are identical or not"
  [key-a key-b]
  (= 0 (compare key-a key-b)))

(defn is-out-by-a-fifth?
  "Tells whether two keys have a distance of 5 semi-tones
  between them or not"
  [key-a key-b]
  (let [value-a (get key-values key-a)
        value-b (get key-values key-b)
        distance (mod (+ 12 (- value-a value-b)) 12)]
    (and 
      (= (get key-types key-a) (get key-types key-b))
      (or (= distance 5) (= distance 7)))))

(defn is-relative? 
  "Tells whether two keys correspond to parallel scales or not"
  [key-a key-b]
  (let [distance (key-distance key-a key-b)]
    (and 
      (= (get key-types key-a) (get key-types key-b))
      (or (= distance 5) (= distance 7)))))

(comment 
  "Generate all the possible profiles by rotating the two existing ones :
  major C -> major C, major C#, ... major B
  minor C -> minor C, minor C#, ... minor B")
(def all-major-profiles
  (doall 
    (map 
      (fn [i] (rotate-left major-base-profile i)) 
      (range 12))))
(def all-minor-profiles
  (doall 
    (map 
      (fn [i] (rotate-left minor-base-profile i)) 
      (range 12))))

(defn match-with-profiles 
  "Matches an input chromatic vector with every major or every minor profiles
  and return all the scores"
  [chromatic-vector profiles]
  (doall (map
    (fn [i] (pearsonr chromatic-vector (nth profiles i)))
    (range 12))))

(defn add-frequency-range-offset
  "Take into account the fact that the first note starts at min-midi-note"
  [key-index]
  (if
    (>= key-index 12)
    (+ 12 (mod (+ key-index (- min-midi-note 1)) 12))
    (mod (+ key-index (- min-midi-note 1)) 12)))

(defn find-best-profile 
  "Matches an input chromatic vector with every major or every minor profiles,
  and returns the key that maximizes the match score."
  [chromatic-vector]
  (let [major-scores (match-with-profiles chromatic-vector all-major-profiles)
        minor-scores (match-with-profiles chromatic-vector all-minor-profiles)
        best-major   (arg-max major-scores)
        best-minor   (arg-max minor-scores)]
    (if 
      (> (nth major-scores best-major) (nth minor-scores best-minor))
      (add-frequency-range-offset (- 12 best-major))
      (add-frequency-range-offset (+ 12 (- 12 best-minor))))))