(ns neuhon.profiles-test
  ^{:doc "Testing how Neuhon finds the best tone profile"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.profiles]
        [neuhon.test-utils]))


(deftest match-exact-same-profiles-test
  (testing "Comparing tone profiles with themselves"
    (is (= (find-best-profile (nth all-major-profiles 0))  0))
    (is (= (find-best-profile (nth all-major-profiles 1))  1))
    (is (= (find-best-profile (nth all-major-profiles 2))  2))
    (is (= (find-best-profile (nth all-major-profiles 3))  3))
    (is (= (find-best-profile (nth all-major-profiles 4))  4))
    (is (= (find-best-profile (nth all-major-profiles 5))  5))
    (is (= (find-best-profile (nth all-major-profiles 6))  6))
    (is (= (find-best-profile (nth all-major-profiles 7))  7))
    (is (= (find-best-profile (nth all-major-profiles 8))  8))
    (is (= (find-best-profile (nth all-major-profiles 9))  9))
    (is (= (find-best-profile (nth all-major-profiles 10)) 10))
    (is (= (find-best-profile (nth all-major-profiles 11)) 11))
    (is (= (find-best-profile (nth all-minor-profiles 0))  12))
    (is (= (find-best-profile (nth all-minor-profiles 1))  13))
    (is (= (find-best-profile (nth all-minor-profiles 2))  14))
    (is (= (find-best-profile (nth all-minor-profiles 3))  15))
    (is (= (find-best-profile (nth all-minor-profiles 4))  16))
    (is (= (find-best-profile (nth all-minor-profiles 5))  17))
    (is (= (find-best-profile (nth all-minor-profiles 6))  18))
    (is (= (find-best-profile (nth all-minor-profiles 7))  19))
    (is (= (find-best-profile (nth all-minor-profiles 8))  20))
    (is (= (find-best-profile (nth all-minor-profiles 9))  21))
    (is (= (find-best-profile (nth all-minor-profiles 10)) 22))
    (is (= (find-best-profile (nth all-minor-profiles 11)) 23))))

(deftest match-exact-same-proportions-test
  (testing "Comparing tone profiles with profiles with same proportions"
    (is (= (find-best-profile 
      (seq [46.2 14.0 24.5 15.4 32.2 28.0 17.5 36.4 16.8 26.6 16.1 23.8])) 0))
    (is (= (find-best-profile 
      (seq [14.0 24.5 15.4 32.2 28.0 17.5 36.4 16.8 26.6 16.1 23.8 46.2])) 1))
    (is (= (find-best-profile 
      (seq [19.5 8.4 10.5 16.2 8.1 10.5 7.5 15.3 12.0 8.1 12.9 9.6])) 12))
    (is (= (find-best-profile 
      (seq [15.3 12.0 8.1 12.9 9.6 19.5 8.4 10.5 16.2 8.1 10.5 7.5])) 19))))

(deftest match-with-noised-profiles-test
  (testing "Comparing tone profiles with noised profiles"
    (is (= (find-best-profile 
      (seq [46.2 14.0 24.5 15.4 32.2 28.0 17.5 36.4 16.8 26.6 16.1 20.8])) 0))
    (is (= (find-best-profile 
      (seq [15.3 12.0 8.1 12.9 9.6 18.6 6.2 8.3 15.4 7.8 9.6 7.2])) 19))
    (is (= (find-best-profile 
      (seq [15.3 12.0 7.2 12.9 9.6 19.5 8.4 10.5 14.3 8.1 10.5 6.1])) 19))
    (is (= (find-best-profile 
      (seq [15.3 12.0 8.1 12.9 9.6 19.5 8.4 10.5 16.2 8.1 0.0 0.0])) 19))))