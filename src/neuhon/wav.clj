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

(def complement-int8  (bit-shift-left 1 7))  ;; TODO
(def complement-int16 (bit-shift-left 1 15)) ;; TODO
(def complement-int24 (bit-shift-left 1 23)) ;; TODO
(def complement-int32 (bit-shift-left 1 31)) ;; TODO

(defn apply-complement [compl condition value]
  (if condition value (- value compl)))

(defn le2c-bytes-to-int8 [^bytes arr ^double ith] ;; little endian, 2's complement
  (apply-complement complement-int8 
    (= 0 (bit-and (aget arr ith) 0x00000080))
    (bit-and (aget arr ith) 0x0000007f)))

(defn le2c-bytes-to-int16 [^bytes arr ^double ith] ;; little endian, 2's complement
  (apply-complement complement-int16
    (= 0 (bit-and (aget arr (+ ith 1)) 0x00008000))
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x00007f00))))

(defn le2c-bytes-to-int24 [^bytes arr ^double ith] ;; little endian, 2's complement
  (apply-complement complement-int24
    (= 0 (bit-and (aget arr (+ ith 2)) 0x00800000))
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
      (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x007f0000))))

(defn le2c-bytes-to-int32 [^bytes arr ^double ith] ;; little endian, 2's complement
  (apply-complement complement-int32
    (= 0 (bit-and (aget arr (+ ith 3)) 0x80000000))
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
      (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x00ff0000)
      (bit-and (bit-shift-left (aget arr (+ ith 3)) 24) 0x7f000000))))

(defn load-wav [filepath & {:keys [rate] :or {rate 44100}}]
  (with-open [r (io/input-stream filepath)]
    (let [data (IOUtils/toByteArray (DataInputStream. r))
          bytes-per-sample (/ (int (aget data bits-per-sample-loc)) 8)
          bytes-per-bloc (le2c-bytes-to-int16 data bytes-per-bloc-loc)
          number-of-channels (int (aget data number-of-channels-loc))
          number-of-bytes (le2c-bytes-to-int32 data number-of-bytes-loc)
          sampling-frequency (le2c-bytes-to-int32 data sampling-frequency-loc)
          relative-rate (float (/ rate sampling-frequency))
          bytes-step (int (* bytes-per-sample number-of-channels (Math/round (/ 1.0 relative-rate))))
          int-converter (cond
            (= bytes-per-sample 3) le2c-bytes-to-int24
            (= bytes-per-sample 2) le2c-bytes-to-int16
            (= bytes-per-sample 1) le2c-bytes-to-int8
            :else le2c-bytes-to-int16)]

        (doall (map (fn [i] (int-converter data i))
          (range wav-header-size number-of-bytes bytes-step))))))