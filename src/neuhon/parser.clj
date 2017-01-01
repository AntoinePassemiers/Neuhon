(require '[clojure.data.csv :as csv]
         '[clojure.java.io :as io])

(def csv-path (str "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"))


(with-open [in-file (io/reader csv-path)]
	(let [csv-seq (csv/read-csv in-file :separator (first ";"))]
		(do (loop [i 1] ;; skip header
			(when (< i (count csv-seq))
        (let [line (nth csv-seq i)
              artist (nth line 0)
              title (nth line 1)
              target-key (nth line 2)
              audio-filename (nth line 3)]
          (println target-key))
				(recur (inc i)))))))
