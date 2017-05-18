(ns neuhon.wav
  ^{:doc "Load a stereo wav file"
    :author "Antoine Passemiers"}
  (:require [clojure.java.io :as io])
  (:import [java.io DataInputStream DataOutputStream]
           [org.apache.commons.io IOUtils])
  (:use [clojure.java.io]
        [neuhon.utils]))

;; Header structure, with a default total size of 44 bytes
;; Each of the values below indicates the location (in bytes) of
;; its corresponding parameter in the raw header
(def bits-per-sample-loc (int 34))    ;; Number of bits per audio sample
(def bytes-per-bloc-loc (int 32))     ;; Number of bytes per audio block
(def number-of-channels-loc (int 22)) ;; Number of channels (2 by default)
(def number-of-bytes-loc (int 4))     ;; Total number of data bytes
(def sampling-frequency-loc (int 24)) ;; Sampling frequency of the signal
(def subchunk-1-size-loc (int 16))    ;; SubChunk1Size

(defn le2c-bytes-to-int8 
  "Converts a byte to a 8-bits integer located at ith in the input stream.
  The sample is a simple 8 bits unsigned integer"
  [^bytes arr ^Integer ith]
  (float
    (aget arr ith)))

(defn le2c-bytes-to-int16 
  "Converts 2 bytes to a 16-bits integer, 
  where the little endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^Integer ith]
  (float
    (-
      (bit-or
        (bit-and (aget arr (+ ith 0)) 0x000000ff)
        (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x00007f00))
      (bit-and (aget arr (+ ith 1)) 0x00008000))))

(defn le2c-bytes-to-int24 
  "Converts 3 bytes to a 24-bits integer, 
  where the little endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^Integer ith]
  (float
    (-
      (bit-or 
        (bit-and (aget arr (+ ith 0)) 0x000000ff)
        (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
        (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x007f0000))
      (bit-and (aget arr (+ ith 2)) 0x00800000))))

(defn le2c-bytes-to-int32 
  "Converts 4 bytes to a 32-bits integer, 
  where the little endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^Integer ith]
  (float
    (-
      (bit-or 
        (bit-and (aget arr (+ ith 0)) 0x000000ff)
        (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
        (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x00ff0000)
        (bit-and (bit-shift-left (aget arr (+ ith 3)) 24) 0x7f000000))
      (bit-and (aget arr (+ ith 3)) 0x80000000))))

(defn load-wav-from-byte-array
  "Efficient way to load a raw audio stream into a Clojure sequence,
  given a filename and a target sampling frequency. For efficiency purposes,
  the samples that do not fall in the range of the target sampling frequency
  are skipped. The target frequency most be a whole divisor of the file's
  sampling frequency."
  [data & {:keys [rate] :or {rate 44100}}]
  ;; Extracts metadata from the raw header
  (let [bytes-per-sample (/ (int (aget data bits-per-sample-loc)) 8)
        bytes-per-bloc (le2c-bytes-to-int16 data bytes-per-bloc-loc)
        number-of-channels (int (aget data number-of-channels-loc))
        number-of-bytes (le2c-bytes-to-int32 data number-of-bytes-loc)
        sampling-frequency (le2c-bytes-to-int32 data sampling-frequency-loc)
        relative-rate (float (/ rate sampling-frequency))
        bytes-step (int (* bytes-per-sample number-of-channels (Math/round (/ 1.0 relative-rate))))
        subchunk1byte (le2c-bytes-to-int32 data subchunk-1-size-loc)
        header-size (cond
          (= subchunk1byte 16) 44
          (= subchunk1byte 18) 46
          :else (+ 28 subchunk1byte))
        int-converter (cond
          (= bytes-per-sample 4) le2c-bytes-to-int32
          (= bytes-per-sample 3) le2c-bytes-to-int24
          (= bytes-per-sample 2) le2c-bytes-to-int16
          (= bytes-per-sample 1) le2c-bytes-to-int8
          :else le2c-bytes-to-int16)
        stereo-to-mono (cond
          (= number-of-channels 1) 
            (fn [i] (float (int-converter data i)))
          (= number-of-channels 2)
            (fn [i] (float (/ 
              (+ (int-converter data i) 
                (int-converter data (+ i bytes-per-sample))) 2)))
          :else
            (fn [i] (float (/ 
              (+ (int-converter data i) 
                (int-converter data (+ i bytes-per-sample))) 2))))]
    (do
      ;; Assumes the sampling frequency (after resampling) is equal to 44100 Hz
      (assert (= (int sampling-frequency) 44100))
      ;; Loads the audio samples and computes the mean of the two channels
      (map 
        stereo-to-mono
        (range 
          (+ 12 header-size)
          number-of-bytes
          bytes-step)))))

(defn load-wav
  "Loading an audio Clojure sequence from a filepath"
  [filepath & {:keys [rate] :or {rate 44100}}]
  (with-open [r (io/input-stream filepath)]
    (load-wav-from-byte-array
      (IOUtils/toByteArray 
        (DataInputStream. r))
      :rate rate)))


;; (def tmp-db-base-path (str "D://KeyFinderDB/"))
;; (def tmp-audio-filename (str "10cc_-_Dreadlock_Holiday.wav"))
;; (def tmp-filepath (clojure.string/join [tmp-db-base-path tmp-audio-filename]))
;; (profile (dotimes [_ 5] (load-wav tmp-filepath :rate 4410.0)))