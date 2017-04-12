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
    #(+ 
      (* chromatic-max-weight (apply max %1))
      (* (- 1.0 chromatic-max-weight) (esum %1)))
    (transpose 
      (object-array 
        (partition 12 12 periodogram)))))