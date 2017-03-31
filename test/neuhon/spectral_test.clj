(ns neuhon.spectral-test
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all]))


(deftest phase-changes-test
  (testing "Phase changes with Lomb-Scargle method"
    (is (= (freq-time-delay 440.0) -2.07954))
    (is (= (freq-time-delay 207.65234) 4.47486))
    ))