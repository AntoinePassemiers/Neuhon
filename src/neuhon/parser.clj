(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])

	 
;; https://github.com/kedean/cfft

(load-file "src/neuhon/wav.clj") ;; TODO
(load-file "src/neuhon/chishi.clj") ;; TODO
(load-file "src/neuhon/windowing.clj") ;; TODO
(load-file "src/neuhon/profiles.clj") ;; TODO

(def db-base-path (str "D://KeyFinderDB/"))
(def csv-path (str "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"))
(def out-path (str "output.txt"))

(deftype Metadata [file-id artist title target-key predicted-key])

(defn display-result [metadata print-function]
  (print-function (format "File number %4d\n" (.file-id metadata)))
  (print-function "----------------\n")
  (print-function (format "Artist        : %s\n" (.artist metadata)))
  (print-function (format "Title         : %s\n" (.title metadata)))
  (print-function (format "Target key    : %s\n" (.target-key metadata)))
  (print-function (format "Predicted key : %s\n\n" (.predicted-key metadata))))

(defn find-key [filepath]
  (let [signal (load-wav filepath :rate sampling-freq-default)
        N (count signal)
        test-signal (drop 460000 signal)
        window (create-window spectrum-size-default nuttall-window-func) ;; Not working
        c (complex-to-real (FFT 
          (new-complex-array test-signal spectrum-size-default) spectrum-size-default))
        cqt (doall (map (fn [i] (apply-win-on-spectrum c i)) 
          (range (count cosine-windows))))
        chromatic-vector (map (fn [i] (note-score cqt i)) (range 12))]
    (do 
      (find-best-profile chromatic-vector))))

(use 'clojure.java.io)
(clojure.java.io/file out-path)
(defn process-all []
  (with-open [in-file (io/reader csv-path)]
    (with-open [wrtr (writer out-path)]
      (let [csv-seq (csv/read-csv in-file :separator (first ";"))
            tp (atom 0)
            fp (atom 0)]
        (do (loop [i 1] ;; skip header
        ;; (when (< i (count csv-seq))
        (when (< i 15)
          (let [line (nth csv-seq i)
                artist (nth line 0)
                title (nth line 1)
                target-key (nth line 2)
                audio-filename (nth line 3)
                audio-filepath (clojure.string/join [db-base-path audio-filename])
                predicted-key (find-key audio-filepath)
                metadata (Metadata. i artist title target-key predicted-key)]
            (do
              (if (= 0 (compare predicted-key target-key)) (swap! tp inc) (swap! fp inc))
              (display-result metadata print)
              (display-result metadata (fn [s] (.write wrtr s)))))
          (recur (inc i))))
        (println @tp)
        (println @fp))))))

(process-all)