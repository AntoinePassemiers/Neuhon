(require '[clojure.java.io :as io])

(import '[org.apache.commons.io IOUtils])

;; Header structure
(def wav-header-size (int 44)) ;; total size : 44 bytes
(def bits-per-sample-loc (int 34))
(def number-of-channels-loc (int 22))


(defn load-wav [filepath]
  (with-open [r (io/input-stream filepath)]
    (let [data (IOUtils/toByteArray (DataInputStream. r))
          bits-per-sample (int (aget data bits-per-sample-loc))
          number-of-channels (int (get data number-of-channels-loc))]
      (println (int (aget data 22))))))