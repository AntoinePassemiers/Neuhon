(ns neuhon.spectral-test
  ^{:doc "Testing the chromatic matrix operations"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.windowing]
        [neuhon.test-utils]))


(deftest note-score-test
  (testing "Score of a given note"
    (is
      (coll-almost-equals
        (reshape-into-chromatic-vector
          (seq [1.0 2.0 3.0 4.0 5.0 6.0 7.0 8.0 9.0 10.0 11.0 12.0 
                5.0 6.0 7.0 2.0 8.0 4.0 6.0 8.0 9.0 9.0  4.0  2.0]))
        (seq [5.2 6.4 7.6 4.4 9.0 6.8 8.2 9.6 10.8 11.8 11.8 12.4])
        2))))