(ns neuhon.fft-test
  ^{:doc "Testing the Fast Fourier Transform implementation"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.fft]
        [neuhon.utils]
        [neuhon.test-utils]))

(deftest bit-reversal-test
  (testing "Bit reversal algorithm"
    (is (= (reverse-bits 5) 2560))
    (is (= (reverse-bits 17) 2176))
    (is (= (reverse-bits 1968) 222))))