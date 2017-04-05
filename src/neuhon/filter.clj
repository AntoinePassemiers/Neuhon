(ns neuhon.filter
  ^{:doc "Low-pass filters"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(defn moving-average-filtering
    "Low-pass filtering using a moving average window"
    [signal cutoff-freq])