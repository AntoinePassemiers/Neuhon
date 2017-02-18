(ns neuhon.utils)

(defn convert-to-array
  "Converts an input sequence to a Java array"
  [sequence length]
  (let [new_array (make-array Double/TYPE length)]
    (doall
      (map
        (fn [i] (aset new_array i (double (nth sequence i))))
        (range length)))
    new_array))