(ns neuhon.core
  (:gen-class)
  (:import (org.jtransforms.fft DoubleFFT_1D))
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [neuhon.wav]
        [neuhon.windowing]
        [neuhon.profiles]
        [neuhon.fft]
        [neuhon.spectral]
        [neuhon.utils]))

;; hardcoded path
;; Base folder where the wav files are located
(def db-base-path (str "D://KeyFinderDB/"))

;; csv file containing the title, artist, wav filename and label of each song
(def csv-path (str db-base-path "DOC/KeyFinderV2Dataset.csv"))

;; output file storing the predicted key signatures
(def out-filename (str "output.txt"))

;; Metadata type for keeping the metadata of a file, as well as its predicted key signature
(deftype Metadata [file-id artist title target-key predicted-key distance])

(defn display-result 
  "Generic print function for displaying a prediction.
  The results can be displayed in a file or in the standard output."
  [metadata print-function]
  (print-function (format "File number %4d\n" (.file-id metadata)))
  (print-function "----------------\n")
  (print-function (format "Artist        : %s\n" (.artist metadata)))
  (print-function (format "Title         : %s\n" (.title metadata)))
  (print-function (format "Target key    : %s\n" (.target-key metadata)))
  (print-function (format "Predicted key : %s (%s)\n\n"
    (.predicted-key metadata) (str (.distance metadata)))))

(defn find-key-locally
  "Predict the key signature of a song at a certain location of it.
  This function must be called multiple times for a same song at 
  different locations to enhance its accuracy. This can be operated
  using a sliding window, and making a weighted average of the local predictions."
  [key-counters signal start]
  (let [window (create-window spectrum-size-default nuttall-window-func) ;; Not stable
        signal (convert-to-complex-array signal start spectrum-size-default)
        c (.realForwardFull (DoubleFFT_1D. spectrum-size-default) signal) ;; Fast Fourier Transform
        ;; c (complex-to-real real imag) ;; Complex spectrum to real spectrum conversion
        cqt (doall ;; Constant-Q Transform
          (map
            (fn [i] (apply-win-on-spectrum c i))
            (range (count cosine-windows))))
        chromatic-vector (map (fn [i] (note-score cqt i)) (range 12))]
    (do
      (find-best-profile chromatic-vector))))

(defn find-key-globally
  "Predict the key signature of a song by making multiple local predictions
  and averaging the results. The process is as follows :
  1) Loading the wav file at a low sampling rate (ex : 4410 Hz)
     Aliasing effects are not taken into account yet
  2) Initialize a sliding window with fixed size (ex : 16384 samples)
  3) Predict the key signature within the temporal window (call find-key-locally)
     - Compute the complex FFT
     - Convert it to a real FFT
     - Convolute the spectrum at logarithmically-spaced bins with spectral windows
       and sum the results to new coefficients
     - Reshape the new coefficients into a matrix of 12 columns
     - Make a weighted sum of the coefficients over the rows -> new chromatic vector
     - Find the profile that matches the chromatic vector the best
     - The best profile indicates which key signature is the most probable
  4) Increment the key counter corresponding to the locally-predicted key
  5) Slide the temporal window
  6) Start over from step 3
  7) Predict using the highest key counter"
  [filepath]
  (let [signal (load-wav filepath :rate sampling-freq-default)
        N (count signal)
        key-counters (make-array data-type 24)
        step-size (int (Math/floor (/ N spectrum-size-default)))]
    ;; (ste signal 4000 spectrum-size-default) ;; Computes short term energy
    (doall
      (map
        (fn [i]
          (do
            (inc-array-element key-counters 
              (find-key-locally key-counters signal i))))
        (range 0 (* step-size spectrum-size-default) spectrum-size-default)))
    (key-to-str (arg-max (seq key-counters)))))

;;(clojure.java.io/file out-filename)
(defn process-all
  "Function for evaluating the final key prediction algorithm.
  This is done by parsing a csv file (containing titles, artists, filenames
  and encoded key signatures), predicting the key signature for each wav file
  and comparing with the real, hand-encoded key signatures."
  [db-path]
  (with-open [in-file (io/reader csv-path)]
    (with-open [wrtr (writer out-filename)]
      (let [csv-seq (csv/read-csv in-file :separator (first ";"))
            perfect-matches (atom 0)
            relative-matches (atom 0)
            parallel-matches (atom 0)
            out-of-a-fifth-matches (atom 0)
            wrong-keys (atom 0)]
        (do (loop [i 1] ;; skip header
        ;; (when (< i (count csv-seq))
        (when (< i 2)
          (let [line (nth csv-seq i)
                artist (nth line 0)
                title (nth line 1)
                target-key (nth line 2)
                audio-filename (nth line 3)
                audio-filepath (clojure.string/join [db-path audio-filename])
                predicted-key (find-key-globally audio-filepath)
                metadata (Metadata. i artist title target-key predicted-key
                  (key-distance predicted-key target-key))]
            (do
              (cond
                (is-same-key? predicted-key target-key) 
                  (swap! perfect-matches inc)
                (is-out-of-a-fifth? predicted-key target-key)
                  (swap! out-of-a-fifth-matches inc)
                ;; (is-relative? predicted-key target-key) 
                ;;   (swap! relative-matches inc)
                :else (swap! wrong-keys inc))
              (display-result metadata print)
              (flush)
              (display-result metadata (fn [s] (.write wrtr s)))))
          (recur (inc i))))
        (println (format "---> Perfect matches        : %4d" @perfect-matches))
        (println (format "---> Out-of-a-fifth matches : %4d" @out-of-a-fifth-matches))
        (println (format "---> Relative matches       : %4d" @relative-matches))
        (println (format "---> Parallel matches       : %4d" @parallel-matches))
        (println (format "---> Wrong predictions      : %4d" @wrong-keys)))))))

(process-all db-base-path)

;; Usefull functions : zipmap, repeat, disj
;; fn + nth -> is it really slower than fn + fn ?