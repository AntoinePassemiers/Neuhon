(ns neuhon.core
  ^{:doc "Main features of the software"
    :author "Antoine Passemiers"}
  (:gen-class)
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

(defn process-all
  "Processing all the wav files contained in folder-path"
  [folder-path & {:keys [threading overlap] :or {threading false overlap 0.0}}]
  (map
    #(try 
      (do
        (println (format "\nProcessing file %s" (str %1)))
        (println 
          (format "\nPredicted key : %s" 
            (find-key-globally (str %1) threading overlap)))
      (do)) ;; TODO : return something useful
      (catch java.io.FileNotFoundException e (do)))
    (file-seq
      (file folder-path))))

;; (process-all-for-evaluation db-base-path)
;; (process-all "D://KeyFinderDB/test" :threading true)

