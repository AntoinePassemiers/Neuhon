(ns neuhon.windowing-test
  ^{:doc "Testing the spectral and temporal windows"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.windowing]
        [neuhon.utils]
        [neuhon.test-utils]))


(deftest blackman-window-test
  (testing "Blackman window"
    (is (coll-almost-equals 
      (map (blackman-window-func 16) (range 16))
      (seq [-1.38777878e-17 1.67577197e-02 7.70724198e-02 2.00770143e-01
             3.94012424e-01 6.30000000e-01 8.49229857e-01 9.82157437e-01
             9.82157437e-01 8.49229857e-01 6.30000000e-01 3.94012424e-01
             2.00770143e-01 7.70724198e-02 1.67577197e-02 -1.38777878e-17])
      3))))

(def get-left-bound
  (fn [freq] 
    (win-left-bound 0.04757048 freq window-size target-sampling-rate)))

(def get-right-bound
  (fn [freq] 
    (win-right-bound 0.04757048 freq window-size target-sampling-rate)))

(deftest window-bounds-test
  (testing "Left and right bounds of a spectral window"
    (is (almost-equals (get-left-bound 440.0) 398.95086 3))
    (is (almost-equals (get-right-bound 440.0) 418.391543 3))
    (is (almost-equals (get-left-bound 311.1270) 282.10085 3))
    (is (almost-equals (get-right-bound 311.1270) 295.847514 3))))

(deftest frequency-bins-test
  (testing "Indexes of the left and right bounds"
    (is (= (nearest-bin-index 282.10085855) 262))
    (is (= (nearest-bin-index 295.84749761) 275))
    (is (= (nearest-bin-index 125.66164663) 117))
    (is (= (nearest-bin-index 131.78507819) 122))))

(defn w-xk
  [signal lk rk]
  (map #(cosine-win-element %1 lk rk) signal))

(deftest cosine-test
  (testing "Cosine window coefficients"
    (is
      (coll-almost-equals
        (w-xk (seq [5.2 4.5 8.5 4.5 6.5 2.5 7.4 2.2]) 53.0 87.5)
        (seq [1.752215 1.8298853 1.2478078 1.8298853 1.5766803
              1.9741991 1.43564097 1.98507016])
        3))))