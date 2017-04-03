(ns neuhon.test-utils
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.spectral]))

(defn is-all-true?
  [coll]
  (every?
    (true? coll)))

(defn is-almost-all-true?
  [coll fraction]
  (>=
    (float
      (/
        (count (filter identity coll))
        (count coll)))
    fraction))

(defn round-as-string 
  [value n-decimals]
  (cond
    (= n-decimals 0) (format "%.0f" (bigdec value))
    (= n-decimals 1) (format "%.1f" (bigdec value))
    (= n-decimals 2) (format "%.2f" (bigdec value))
    (= n-decimals 3) (format "%.3f" (bigdec value))
    :else (format "%.4f" (bigdec value))))

(defn almost-equals
  [a b n-decimals]
  (=
    (round-as-string a n-decimals)
    (round-as-string b n-decimals)))

(defn coll-almost-equals
  ([A B n-decimals]
    (is-all-true?
      (map #(almost-equals %1 %2 n-decimals) A B)))
  ([A B n-decimals fraction]
    (is-almost-all-true?
      (map #(almost-equals %1 %2 n-decimals) A B)
      fraction)))