(ns neuhon.core
  ^{:doc "Evaluating the performance of critical parts of the code"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:require [taoensso.tufte :as tufte :refer (defnp p profiled profile)])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.utils]))

(tufte/add-basic-println-handler! {})

(def frame (range 16384))

(defn profile-complex-conversion
  []
  (do
    (profile {}
      (dotimes [_ 20]
        (p :convert-to-complex-array 
          (convert-to-complex-array 
            frame))))
    (profile {}
      (dotimes [_ 20]
        (p :convert-to-complex-array 
          (double-array (* 2 16384)
            frame))))))

(profile-complex-conversion)