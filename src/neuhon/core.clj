(ns neuhon.core
  ^{:doc "Main features of the software"
    :author "Antoine Passemiers"}
  (:gen-class)
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io])
  (:use [clojure.java.io]
        [clojure.core.matrix]
        [clj-progress.core]
        [neuhon.wav]
        [neuhon.windowing]
        [neuhon.matrix]
        [neuhon.profiles]
        [neuhon.spectral]
        [neuhon.fft]
        [neuhon.markov]
        [neuhon.utils]))

;; hardcoded path
;; Base folder where the wav files are located
(def db-base-path (str "D://KeyFinderDB/"))

;; csv file containing the title, artist, wav filename and label of each song
(def csv-path (str db-base-path "DOC/KeyFinderV2Dataset.csv"))

;; output file storing the predicted key signatures
(def out-filename (str "output.txt"))

;; Value to use for padding while partitioning the signal into windows
(def ^:const padding-default-value 0)

;; Metadata type for keeping the metadata of a file, as well as its predicted key signature
(deftype Metadata [file-id artist title target-key predicted-key distance])

(defn display-result 
  "Generic print function for displaying a prediction.
  The results can be displayed in a file or in the standard output."
  [metadata print-function]
  (print-function (format "\nFile number %4d\n" (.file-id metadata)))
  (print-function "----------------\n")
  (print-function (format "Artist        : %s\n" (.artist metadata)))
  (print-function (format "Title         : %s\n" (.title metadata)))
  (print-function (format "Target key    : %s\n" (.target-key metadata)))
  (print-function (format "Predicted key : %s (%s)\n\n"
    (.predicted-key metadata) (str (.distance metadata)))))

(defn extract-dsk-transform
  "Compute the direct spectral kernel transform of
  an input sequence"
  [frame]
  (let [frame-array (to-complex-array frame)]
    (do
      (take ;; TODO
        window-size
        (dsk-transform
          (complex-to-real-seq
            (iterative-radix2-fft frame-array)))))))

(defn extract-chromatic-vector
  "Compute the chromatic vector of a song's segment"
  [signal use-cqt?]
  (if
    use-cqt?
    ;; Compute the Constant-Q Transform
    (reshape-into-chromatic-vector
      (extract-dsk-transform signal))
    ;; Compute the Lomb-Scargle periodogram
    (reshape-into-chromatic-vector
      (compute-periodogram signal))))

(defn find-key-locally
  "Predict the key signature of a song at a certain location of it.
  This function must be called multiple times for a same song at 
  different locations to enhance its accuracy. This can be operated
  using a sliding window, and making a weighted average of the local predictions."
  [signal use-cqt?]
  (find-best-profile
    (extract-chromatic-vector signal use-cqt?)))

(defn find-key-globally
  "Predict the key signature of a song by making multiple local predictions
  and averaging the results. The process is as follows :
  1) Loading the wav file at a low sampling rate (ex : 4410 Hz)
     Aliasing effects are not taken into account yet
  2) Initialize a sliding window with fixed size (ex : 16384 samples)
  3) Predict the key signature within the temporal window (call find-key-locally)
     - Overlay the current frame on the next one and compute the sum
     - Compute the periodogram
     - Reshape the new coefficients into a matrix of 12 columns
     - Make a weighted sum of the coefficients over the rows -> new chromatic vector
     - Find the profile that matches the chromatic vector the best
     - The best local profile indicates which key signature is the most probable
     - Combine the local predictions using Markov chains
  4) Increment the key counter corresponding to the locally-predicted key
  5) Slide the temporal window
  6) Start over from step 3
  7) Predict using the highest key counter"
  ([filepath]
    (find-key-globally filepath false 0.0))
  ([filepath threading? use-cqt? overlap]
    (let [signal (load-wav filepath :rate target-sampling-rate)
          step (int (* window-size (- 1.0 overlap)))
          partitions (partition ;; TODO
            window-size
            step
            (repeat window-size padding-default-value)
            signal)
          n-steps (count partitions)
          umap (if threading? pmap map)]
      (do
        (assert (<= 0.0 overlap 0.8))
        (init (+ 1 n-steps)) ;; Init progress bar
          (key-to-str
            (most-probable-key
              (doall
                (umap
                  (fn [signal-partition]
                    (do
                      (tick) ;; Update progress bar
                      (find-key-locally 
                        signal-partition
                        use-cqt?)))
                  partitions))))))))

(defn process-all-for-evaluation
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
            out-by-a-fifth-matches (atom 0)
            wrong-keys (atom 0)]
        (do (loop [i 1] ;; skip header
        ;; (when (< i (count csv-seq))
        (when (< i 2) ;; 230
          (try
            (let [line (nth csv-seq i)
                  artist (nth line 0)
                  title (nth line 1)
                  target-key (nth line 2)
                  audio-filename (nth line 3)
                  audio-filepath (clojure.string/join [db-path audio-filename])
                  predicted-key (find-key-globally audio-filepath true true 0.0)
                  metadata (Metadata. i artist title target-key predicted-key
                    (key-distance predicted-key target-key))]
              (do
                (cond
                  (is-same-key? predicted-key target-key)
                    (swap! perfect-matches inc)
                  (is-out-by-a-fifth? predicted-key target-key)
                    (swap! out-by-a-fifth-matches inc)
                  (is-relative? predicted-key target-key) 
                    (swap! relative-matches inc)
                  (is-parallel? predicted-key target-key) 
                    (swap! parallel-matches inc)
                  :else (swap! wrong-keys inc))
                (display-result metadata print)
                (flush)
                (display-result metadata (fn [s] (.write wrtr s)))))
            (catch java.io.FileNotFoundException e 
              (println "Warning : file not found")))
          (recur (inc i))))
        (println (format "---> Perfect matches        : %4d" @perfect-matches))
        (println (format "---> Out-by-a-fifth matches : %4d" @out-by-a-fifth-matches))
        (println (format "---> Relative matches       : %4d" @relative-matches))
        (println (format "---> Parallel matches       : %4d" @parallel-matches))
        (println (format "---> Wrong predictions      : %4d" @wrong-keys)))))))

(defn process-all
  "Processing all the wav files contained in folder-path"
  [folder-path & 
    {:keys [threading? use-cqt? overlap] 
     :or {threading? false use-cqt? false overlap 0.0}}]
  (map
    #(try 
      (do
        (println (format "\nProcessing file %s" (str %1)))
        (println 
          (format "\nPredicted key : %s" 
            (find-key-globally (str %1) threading? use-cqt? overlap)))
      (do)) ;; TODO : return something useful
      (catch java.io.FileNotFoundException e (do)))
    (file-seq
      (file folder-path))))

(process-all-for-evaluation db-base-path)
;; (process-all "D://KeyFinderDB/test" :threading? false :use-cqt? true :overlap 0.0)