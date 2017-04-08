(ns neuhon.core
  ^{:doc "Main features of the software"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:import (org.jtransforms.fft DoubleFFT_1D))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.wav]
        [neuhon.windowing]
        [neuhon.matrix]
        [neuhon.profiles]
        [neuhon.fft]
        [neuhon.spectral]
        [neuhon.validation]
        [neuhon.utils]))

(process-all-for-evaluation db-base-path)