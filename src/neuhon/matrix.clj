(ns neuhon.matrix
  ^{:doc "Defining matrix operations"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(defn reshape-into-chromatic-vector
  "Converts a spectrum/periodogram to a chromatic vector"
  [periodogram]
  (map 
    #(+ (* 0.8 (apply max %1)) (* 0.2 (esum %1)))
    (transpose 
      (object-array 
        (partition 12 12 periodogram)))))

(partition 
  spectrum-size-default
  spectrum-size-default
  [padding-default-value]
  signal)