(ns neuhon.markov
  ^{:doc "Markov chains for temporal analysis of tone variations"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:require [clojure.java.io :as io])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

;; TODO : fitting Markov chains to tone sequences

(def bach-minor-probs
  (object-array
    [[0.00, 0.18, 0.01, 0.20, 0.41, 0.09, 0.12],
     [0.01, 0.00, 0.03, 0.00, 0.89, 0.00, 0.07],
     [0.06, 0.06, 0.00, 0.25, 0.19, 0.31, 0.13],
     [0.22, 0.14, 0.00, 0.00, 0.48, 0.00, 0.15],
     [0.80, 0.00, 0.02, 0.06, 0.00, 0.10, 0.02],
     [0.03, 0.54, 0.03, 0.14, 0.19, 0.00, 0.08],
     [0.81, 0.00, 0.01, 0.03, 0.15, 0.00, 0.00]]))

(def bach-major-probs
  (object-array
    [[0.00, 0.15, 0.01, 0.28, 0.41, 0.09, 0.06],
     [0.01, 0.00, 0.00, 0.00, 0.71, 0.01, 0.25],
     [0.03, 0.03, 0.00, 0.52, 0.06, 0.32, 0.02],
     [0.22, 0.13, 0.00, 0.00, 0.39, 0.02, 0.23],
     [0.82, 0.01, 0.00, 0.07, 0.00, 0.09, 0.00],
     [0.15, 0.29, 0.05, 0.11, 0.32, 0.00, 0.09],
     [0.91, 0.00, 0.01, 0.02, 0.04, 0.03, 0.00]]))