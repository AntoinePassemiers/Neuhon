(require '[clojure.java.io :as io])

(import '[org.apache.commons.io IOUtils])

;; Header structure
(def wav-header-size (int 44)) ;; total size : 44 bytes
(def bits-per-sample-loc (int 34))
(def number-of-channels-loc (int 22))
(def number-of-bytes-loc (int 40))


(defn le-bytes-to-int32 [arr ith] ;; little endian
  (bit-or (bit-and (aget arr (+ ith 3)) 0x000000ff)
    (bit-and (bit-shift-left (aget arr (+ ith 2)) 8) 0x0000ff00)
    (bit-and (bit-shift-left (aget arr (+ ith 1)) 16) 0x00ff0000)
    (bit-and (bit-shift-left (aget arr ith) 24) 0xff000000)))

(defn load-wav [filepath]
  (with-open [r (io/input-stream filepath)]
    (let [data (IOUtils/toByteArray (DataInputStream. r))
          bits-per-sample (int (aget data bits-per-sample-loc))
          number-of-channels (int (get data number-of-channels-loc))
          number-of-bytes-loc (le-bytes-to-int32 data number-of-bytes-loc)]
      (println number-of-bytes-loc))))