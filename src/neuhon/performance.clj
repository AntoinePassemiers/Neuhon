(ns neuhon.core
  ^{:doc "Evaluating the performance of critical parts of the code"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:import (org.jtransforms.fft DoubleFFT_1D))
  (:require [taoensso.tufte :as tufte :refer (defnp p profiled profile)])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.fft]
        [neuhon.spectral]
        [neuhon.utils]))

(tufte/add-basic-println-handler! {})

(def jtransformer (new DoubleFFT_1D 4096))

(def frame (range 4096))

(defn profile-fft
  []
  (do
    (profile {}
      (dotimes [_ 100]
        (p :jtransforms-fft 
          (.realForward 
            jtransformer
            (double-array 8192 frame)))))
    (profile {}
      (dotimes [_ 100]
        (p :clojure-fft
          (iterative-radix2-fft
            (to-complex-array frame)))))
    (profile {}
      (dotimes [_ 100]
        (p :lomb-scargle
          (compute-periodogram
            (into-array Double/TYPE frame)))))))

(defn profile-real-to-complex-conversion
  []
  (do
    (profile {}
      (dotimes [_ 100]
        (p :double-array
          (double-array 8192 frame))))
    (profile {}
      (dotimes [_ 100]
        (p :custom-conversion
          (to-complex-array frame))))))

(profile-fft)