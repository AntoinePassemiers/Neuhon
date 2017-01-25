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

(def note-names ["C" "C#" "D" "Eb" "E" "F" "F#" "G" "G#" "A" "Bb" "B"])

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
        (nth note-names best-major)
        (str (nth note-names best-minor) "m")))))