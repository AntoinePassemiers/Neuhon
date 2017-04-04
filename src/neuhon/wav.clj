(ns neuhon.wav
  ^{:doc "Load a stereo wav file"
    :author "Antoine Passemiers"}
  (:require [clojure.java.io :as io])
  (:import [java.io DataInputStream DataOutputStream]
           [org.apache.commons.io IOUtils])
  (:use [clojure.java.io]))

;; Primitive type of the audio raw samples
(def wav-data-type Integer/TYPE)

;; Header structure, with a default total size of 44 bytes
;; Each of the values below indicates the location (in bytes) of
;; its corresponding parameter in the raw header
(def wav-header-size (int 44))        ;; Total size (44 bytes)
(def bits-per-sample-loc (int 34))    ;; Number of bits per audio sample
(def bytes-per-bloc-loc (int 32))     ;; Number of bytes per audio block
(def number-of-channels-loc (int 22)) ;; Number of channels (2 by default)
(def number-of-bytes-loc (int 4))     ;; Total number of data bytes
(def sampling-frequency-loc (int 24)) ;; Sampling frequency of the signal

(defn le2c-bytes-to-int8 
  "Converts a byte to a 8-bits integer located at ith in the input stream,
  using the two's complement method"
  [^bytes arr ^double ith]
  (-
    (bit-and (aget arr (+ ith 0)) 0x0000007f)
    (bit-and (aget arr (+ ith 0)) 0x00000080)))

(defn le2c-bytes-to-int16 
  "Converts 2 bytes to a 16-bits integer, 
  where the big endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^double ith]
  (-
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x00007f00))
    (bit-and (aget arr (+ ith 1)) 0x00008000)))

(defn le2c-bytes-to-int24 
  "Converts 3 bytes to a 24-bits integer, 
  where the big endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^double ith]
  (-
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
      (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x007f0000))
    (bit-and (aget arr (+ ith 2)) 0x00800000)))

(defn le2c-bytes-to-int32 
  "Converts 4 bytes to a 32-bits integer, 
  where the big endian is located at ith in the input stream,
  and using the two's complement method"
  [^bytes arr ^double ith]
  (-
    (bit-or (bit-and (aget arr (+ ith 0)) 0x000000ff)
      (bit-and (bit-shift-left (aget arr (+ ith 1)) 8) 0x0000ff00)
      (bit-and (bit-shift-left (aget arr (+ ith 2)) 16) 0x00ff0000)
      (bit-and (bit-shift-left (aget arr (+ ith 3)) 24) 0x7f000000))
    (bit-and (aget arr (+ ith 3)) 0x80000000)))

(defn load-wav
  "Efficient way to load a raw audio stream into a Clojure array,
  given a filename and a target sampling frequency. For efficiency purposes,
  the samples that do not fall in the range of the target sampling frequency
  are skipped. The target frequency most be a whole unmber divisor of the file
  sampling frequency."
  [filepath & {:keys [rate] :or {rate 44100}}]
  (with-open [r (io/input-stream filepath)]
    ;; Extracts metadata from the raw header
    (let [data (IOUtils/toByteArray (DataInputStream. r))
          bytes-per-sample (/ (int (aget data bits-per-sample-loc)) 8)
          bytes-per-bloc (le2c-bytes-to-int16 data bytes-per-bloc-loc)
          number-of-channels (int (aget data number-of-channels-loc))
          number-of-bytes (- (le2c-bytes-to-int32 data number-of-bytes-loc) wav-header-size)
          sampling-frequency (le2c-bytes-to-int32 data sampling-frequency-loc)
          relative-rate (float (/ rate sampling-frequency))
          bytes-step (int (* bytes-per-sample number-of-channels (Math/round (/ 1.0 relative-rate))))
          half-bytes-step (/ bytes-step 2)
          int-converter (cond
            (= bytes-per-sample 3) le2c-bytes-to-int24
            (= bytes-per-sample 2) le2c-bytes-to-int16
            (= bytes-per-sample 1) le2c-bytes-to-int8
            :else le2c-bytes-to-int16)]
      (do
        (println bytes-per-sample)
        ;; Assumes the sampling frequency (after resampling) is equal to 44100 Hz
        (assert (= (int sampling-frequency) 44100))
        ;; Assumes the audio has been recorded in stereo mode (2 channels)
        (assert (= number-of-channels 2))
        ;; Loads the audio samples and computes the mean of the two channels
        (doall
          (map 
            (fn [i] (int (/ 
              (+ (int-converter data i) 
                (int-converter data (+ i half-bytes-step))) 2)))
            (range wav-header-size number-of-bytes bytes-step)))))))


(def db-base-path (str "D://KeyFinderDB/"))
(def audio-filename (str "10cc - Dreadlock Holiday.wav"))
(def filepath (clojure.string/join [db-base-path audio-filename]))
(def out-filename (str "wav.txt"))
(with-open [wrtr (writer out-filename)]
  (.write wrtr 
      (pr-str
        (load-wav filepath :rate 4410.0))))