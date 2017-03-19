(ns neuhon.core-test
  (:import (org.jtransforms.fft DoubleFFT_1D))
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all]))

(deftest a-test
  (testing "FIXME, I fail."
    (is (= 0 1))))

(deftest fft-speed
	(let [data (make-array Double/TYPE 16384)]
		(time
			(for [i (range 80)]
				(.realForwardFull 
					(DoubleFFT_1D. spectrum-size-default) signal)))))
