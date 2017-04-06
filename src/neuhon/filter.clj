(ns neuhon.filter
  ^{:doc "Low-pass filters"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(defn moving-average-filtering
  "Low-pass filtering using a moving average window"
  [signal N]
  (map
    #(float
      (/ 
        (apply-sum %1) N))
    (partition N 1 signal)))

;;(doall 
;;  (moving-average-filtering
;;    (take 10000000 (repeatedly #(rand-int 42)))
;;    5))