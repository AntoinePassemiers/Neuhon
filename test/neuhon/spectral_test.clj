(ns neuhon.spectral-test
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.spectral]
        [neuhon.test-utils]))


(deftest phase-changes-test
  (testing "Phase changes with Lomb-Scargle method"
    (is (almost-equals (freq-time-delay 440.0)     -2.07954 3))
    (is (almost-equals (freq-time-delay 207.65234)  4.47486 3))
    (is (almost-equals (freq-time-delay 739.98885)  0.09950 3))
    (is (almost-equals (freq-time-delay 77.781746) -1.23201 3))
    (is (almost-equals (freq-time-delay 329.62756) -2.97568 3))))

(deftest periodogram-test
  (testing "Periodogram of a 440 Hz sine-wave with no phase change"
    (let [test-waveform (cos-waveform 440.0 (freq-time-delay 440.0))]
      (is
        (coll-almost-equals
          (compute-periodogram test-waveform)
          (seq
            [1.35269602E-05 2.65298176E-04 3.94780978E-04 3.68102717E-04
             1.16193042E-04 2.05282314E-04 9.40179396E-05 3.02810225E-04
             2.36882601E-04 1.67765247E-05 3.94877907E-04 3.82539992E-04
             4.20428500E-04 3.84392488E-05 3.33129043E-04 2.45408418E-04
             1.12671471E-04 1.47312841E-05 1.62577501E-04 8.89943313E-05
             1.77483817E-05 4.13223706E-04 3.32549349E-04 2.87845717E-04
             4.29507066E-04 3.36300267E-04 1.49154171E-04 2.12183292E-05
             1.26314986E-04 4.36247090E-04 4.56981979E-05 1.87645737E-04
             4.34043106E-04 4.00220522E-04 1.43818517E-04 5.96421116E-05
             4.68346978E-04 1.50577218E-04 7.38369512E-05 4.65540560E-04
             1.24005013E-04 5.00651727E-04 3.96308787E-04 3.33019051E-05
             4.93011629E-04 3.38758244E-04 1.25130015E-04 4.23480445E-04
             6.77718101E-04 1.34264644E-04 4.57522678E-04 6.94141817E-04
             3.06463441E-04 9.85125030E-04 3.22351127E-04 1.45883179E-03
             1.08322999E-03 2.00658836E-05 1.44895978E-03 2.53859597E-04
             1.01455425E-02 2.67573942E-02 4.09566357E+03 1.43072388E-02
             2.61295843E-03 2.30739179E-03 4.11106521E-04 8.94056096E-04
             3.55143492E-05 5.56798923E-03 8.61840524E-05 2.83516176E-04])
          2
          0.90)))))