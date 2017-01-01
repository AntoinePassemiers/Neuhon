(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])

(def db-base-path (str "D://KeyFinderDB/"))
(def csv-path (str "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"))
(def out-path (str "output.txt"))

(use 'clojure.java.io)
(clojure.java.io/file out-path)
(with-open [in-file (io/reader csv-path)]
  (with-open [wrtr (writer out-path)]
  	(let [csv-seq (csv/read-csv in-file :separator (first ";"))
          tp (atom 0)
          fp (atom 0)]
  		(do (loop [i 1] ;; skip header
  			(when (< i (count csv-seq))
          (let [line (nth csv-seq i)
                artist (nth line 0)
                title (nth line 1)
                target-key (nth line 2)
                audio-filename (nth line 3)
                aufio-filepath (clojure.string/join [db-base-path audio-filename])
                predicted-key "Em"] ;; TODO : call (find-key aufio-filepath)
            (do 
              (if (= 0 (compare predicted-key target-key)) (swap! tp inc) (swap! fp inc))
              (.write wrtr (format "File number %4d\n" i))
              (.write wrtr "----------------\n")
              (.write wrtr (format "Artist        : %s\n" artist))
              (.write wrtr (format "Title         : %s\n" title))
              (.write wrtr (format "Target key    : %s\n" target-key))
              (.write wrtr (format "Predicted key : %s\n\n" predicted-key))))
  				(recur (inc i))))
        (println @tp)
        (println @fp)
      ))))
