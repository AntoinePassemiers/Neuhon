(require '[clojure.java.io :as io])

(import '[java.io DataInputStream DataOutputStream])
(import '[org.apache.commons.io IOUtils])


(def wav-data-type Integer/TYPE)

;; Header structure
(def wav-header-size (int 44)) ;; total size : 44 bytes
(def bits-per-sample-loc (int 34))
(def bytes-per-bloc-loc (int 32))
(def number-of-channels-loc (int 22))
(def number-of-bytes-loc (int 4))
(def sampling-frequency-loc (int 24))

(defn le-bytes-to-int8 [^bytes arr ^double ith] ;; little endian
  (int (aget arr ith)))

(defn le-bytes-to-int16 [^bytes arr ^double ith] ;; little endian
  (bit-or (bit-and (aget arr ith) 0x000000ff)
    (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)))

(defn le-bytes-to-int24 [^bytes arr ^double ith] ;; little endian
  (bit-or (bit-and (aget arr ith) 0x000000ff)
    (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
    (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x00ff0000)))

(defn le-bytes-to-int32 [^bytes arr ^double ith] ;; little endian
  (bit-or (bit-and (aget arr ith) 0x000000ff)
    (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
    (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x00ff0000)
    (bit-and (bit-shift-left (aget arr (+ ith 3)) 24) 0xff000000)))

(defn load-wav [filepath & {:keys [rate] :or {rate 44100}}]
  (with-open [r (io/input-stream filepath)]
    (let [data (IOUtils/toByteArray (DataInputStream. r))
          bytes-per-sample (/ (int (aget data bits-per-sample-loc)) 8)
          bytes-per-bloc (le-bytes-to-int16 data bytes-per-bloc-loc)
          number-of-channels (int (aget data number-of-channels-loc))
          number-of-bytes (le-bytes-to-int32 data number-of-bytes-loc)
          sampling-frequency (le-bytes-to-int32 data sampling-frequency-loc)
          relative-rate (float (/ rate sampling-frequency))
          bytes-step (* bytes-per-sample number-of-channels)
          number-of-elements (int (* relative-rate (float (/ number-of-bytes bytes-per-sample number-of-channels))))
          int-converter (cond
                          (= bytes-per-sample 3) le-bytes-to-int24
                          (= bytes-per-sample 2) le-bytes-to-int16
                          (= bytes-per-sample 1) le-bytes-to-int8
                          :else le-bytes-to-int24)
          signal (make-array wav-data-type number-of-elements)]
      (do 
        (println bytes-per-bloc)
        (loop [i (/ wav-header-size bytes-step)] ;; skip header
          (when (< i number-of-elements) 
            (aset signal i 
              (int-converter data (* i bytes-step)))
            (recur (inc i))))
        signal))))