(ns neuhon.stats
  ^{:doc "Major and minor scale profiles"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(defn mean
  "Mean of an input sequence"
  [coll]
  (/ (apply-sum coll) (count coll)))

(defn variance
  "Variance of an input sequence"
  [coll]
  (/
    (apply-sum
      (map
        #(pow2 (- %1 (mean coll)))
        coll))
    (count coll)))

(defn stdv
  "Standard deviation of an input sequence"
  [coll]
  (Math/sqrt (variance coll)))

(defn normalize
  "Normalizing an input sequence by subtracting its mean"
  [coll]
  (map
    #(- %1 (mean coll))
    coll))

(defn dot-product
  "Dot-product between two input sequences"
  [A B]
  (apply-sum (map #(* %1 %2) A B)))

(defn pearsonr
  "Pearson correlation coeffient between a chromatic vector and a tone profile"
  [chromatic-vector profile]
  (try
    (if 
      (zero? (apply-sum chromatic-vector))
      0.0
      (/
        (dot-product
          (normalize chromatic-vector)
          (normalize profile))
        (*
          (count chromatic-vector)
          (stdv chromatic-vector)
          (stdv profile))))
    (catch ArithmeticException e (float 0.0))))