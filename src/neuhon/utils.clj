(ns neuhon.utils)

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