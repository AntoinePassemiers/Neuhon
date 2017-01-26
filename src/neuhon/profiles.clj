(defn rotate-left 
  ([profile] (rotate-left profile 1))
  ([profile n]
    (if (> n 0) 
      (rotate-left (concat (drop 1 profile) [(first profile)]) (- n 1))
      profile)))

(defn sum [data]
  (reduce + data))

(defn normalize [data]
  (let [total (sum data)]
    (map (fn [i] (/ (nth data i) total)) (range (count data)))))

(def key-names ["C" "C#" "D" "Eb" "E" "F" "F#" "G" "G#" "A" "Bb" "B"])

(def key-values 
  (hash-map "C" 0 "C#" 1 "D" 2 "Eb" 3 "E" 4 "F" 5 "F#" 6 "G" 7 "G#" 8 "A" 9 "Bb" 10 "B" 11
    "Cm" 0 "C#m" 1 "Dm" 2 "Ebm" 3 "Em" 4 "Fm" 5 "F#m" 6 "Gm" 7 "G#m" 8 "Am" 9 "Bbm" 10 "Bm" 11))

(def key-types
  (hash-map "C" 0 "C#" 0 "D" 0 "Eb" 0 "E" 0 "F" 0 "F#" 0 "G" 0 "G#" 0 "A" 0 "Bb" 0 "B" 0
    "Cm" 1 "C#m" 1 "Dm" 1 "Ebm" 1 "Em" 1 "Fm" 1 "F#m" 1 "Gm" 1 "G#m" 1 "Am" 1 "Bbm" 1 "Bm" 1))

(defn key-distance [key-a key-b]
    (let [value-a (get key-values key-a)
          value-b (get key-values key-b)]
      (mod (+ 12 (- value-a value-b)) 12)))

(defn is-same-key? [key-a key-b]
  (= 0 (compare key-a key-b)))

(defn is-left-right-neighboor? [key-a key-b]
  (let [value-a (get key-values key-a)
        value-b (get key-values key-b)
        distance (mod (+ 12 (- value-a value-b)) 12)]
    (and 
      (= (get key-types key-a) (get key-types key-b))
      (or (= distance 5) (= distance 7)))))

(defn is-radial-neighboor? [key-a key-b] ;; TODO
  (let [distance (key-distance key-a key-b)]
    (and 
      (= (get key-types key-a) (get key-types key-b))
      (or (= distance 5) (= distance 7)))))

(def major-base-profile (normalize [6.4 2.2 3.5 2.3 4.4 4.1 2.5 5.2 2.4 3.7 2.3 2.9]))

(def minor-base-profile (normalize [6.4 2.8 3.6 5.4 2.7 3.6 2.6 4.8 4.0 2.7 3.3 3.2]))

(def all-major-profiles
  (doall (map (fn [i] (rotate-left major-base-profile i)) (range 12))))

(def all-minor-profiles
  (doall (map (fn [i] (rotate-left minor-base-profile i)) (range 12))))

(defn dot-product [chromatic-vector profile]
  (let [convolution (fn [i] (* (nth chromatic-vector i) (nth profile i)))]
    (reduce + (map convolution (range 12)))))

(defn match-with-profiles [chromatic-vector profiles]
  (doall (map
    (fn [i] (dot-product chromatic-vector (nth profiles i)))
    (range 12))))

(defn find-best-profile [chromatic-vector]
  (let [major-scores (match-with-profiles chromatic-vector all-major-profiles)
        minor-scores (match-with-profiles chromatic-vector all-minor-profiles)
        best-major   (arg-max major-scores)
        best-minor   (arg-max minor-scores)]
    (do
      (if (> (nth major-scores best-major) (nth minor-scores best-minor))
        (nth key-names best-major)
        (str (nth key-names best-minor) "m")))))