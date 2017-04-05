(ns neuhon.profiles-test
  ^{:doc "Testing how Neuhon finds the best tone profile"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.profiles]
        [neuhon.stats]
        [neuhon.test-utils]))

(deftest key-to-str-test
  (testing "Converting an integer key to a tone name"
    (is (= (key-to-str 0)  "C"))
    (is (= (key-to-str 1)  "C#"))
    (is (= (key-to-str 2)  "D"))
    (is (= (key-to-str 3)  "Eb"))
    (is (= (key-to-str 4)  "E"))
    (is (= (key-to-str 5)  "F"))
    (is (= (key-to-str 6)  "F#"))
    (is (= (key-to-str 7)  "G"))
    (is (= (key-to-str 8)  "G#"))
    (is (= (key-to-str 9)  "A"))
    (is (= (key-to-str 10) "Bb"))
    (is (= (key-to-str 11) "B"))
    (is (= (key-to-str 12) "Cm"))
    (is (= (key-to-str 13) "C#m"))
    (is (= (key-to-str 14) "Dm"))
    (is (= (key-to-str 15) "Ebm"))
    (is (= (key-to-str 16) "Em"))
    (is (= (key-to-str 17) "Fm"))
    (is (= (key-to-str 18) "F#m"))
    (is (= (key-to-str 19) "Gm"))
    (is (= (key-to-str 20) "G#m"))
    (is (= (key-to-str 21) "Am"))
    (is (= (key-to-str 22) "Bbm"))
    (is (= (key-to-str 23) "Bm"))))

(deftest variance-test
  (testing "Variance of an input sequence"
    (is (almost-equals (variance (seq [5.2 1.0 7.4])) 7.04888888 3))
    (is (not (almost-equals (variance (seq [78.0 1.0 7.4])) 7.04888888 3)))))

(deftest normalize-test
  (testing "Normalization of an input sequence"
    (is 
      (=
        (normalize (seq [1.0 3.0 2.0]))
        (seq [-1.0 1.0 0.0])))))

(deftest pearsonr-test
  (testing "Pearson correlation coefficient"
    (is (almost-equals 
      0.2706698 
      (pearsonr
        (seq [1.0 2.0 3.0 4.0])
        (seq [7.8 1.1 3.6 9.7]))
      4))))

(deftest rotate-left-test
  (testing "Rotate an input sequence to the left"
    (is (=
      (rotate-left (seq [4 8 7 6 3 0 0 1 5 7 9 6]) 4)
      (seq [3 0 0 1 5 7 9 6 4 8 7 6])))))

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