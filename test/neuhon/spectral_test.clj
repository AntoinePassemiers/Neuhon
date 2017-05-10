(ns neuhon.spectral-test
  ^{:doc "Testing the Lomb-Scargle algorithm"
    :author "Antoine Passemiers"}
  (:require [clojure.test :refer :all]
            [neuhon.core :refer :all])
  (:use [neuhon.spectral]
        [neuhon.matrix]
        [neuhon.test-utils]))

(deftest round-test
  (testing "Rounding doubles"
    (is (=
      (map 
        #(round-to-n-digits %1 6) 
        (seq [1.35269602E-05 2.65298176E-04 3.94780978E-04 3.68102717E-04]))
      (seq [1.4E-05 2.65E-04 3.95E-04 3.68E-04])))))

(deftest phase-changes-test
  (testing "Phase changes with Lomb-Scargle method"
    (is (almost-equals (freq-time-delay 440.0)     -2.147727 3))
    (is (almost-equals (freq-time-delay 87.307058)  1.789099 3))
    (is (almost-equals (freq-time-delay 246.94165)  2.705212 3))
    (is (almost-equals (freq-time-delay 369.99442) -2.584957 3))
    (is (almost-equals (freq-time-delay 587.32954)  1.416964 3))))

(deftest periodogram-test
  (testing "Periodogram of a 440 Hz sine-wave with no phase change"
    (let [test-waveform (sin-waveform 440.0 0)]
      (is
        (coll-almost-equals
          (compute-periodogram test-waveform)
          (seq
            [0.00030149543144876597,  0.0017556360186915501,  0.0010134703340517819, 
             4.3244895780004861e-05, 0.00049795844788269659,  0.0012848782704874969, 
             0.0016810331859155819,    0.001776392361576477,  0.0017555955174763447, 
             0.0015375727541389666,  0.00084392767258148526, 5.1502583664384221e-05, 
             0.00089791510876860729,  0.0016487494172604806, 4.8742501789667464e-05, 
             0.0017754993914937747,    0.000431763754579093, 0.00035665561171406188, 
             0.001379020600073493,     0.001720257313148792,  0.0016424329936027078, 
             0.00092500274971177722, 4.5132236168338359e-05,  0.0017771823283738871, 
             3.551754531971883e-05,   0.0012605351251509459,  0.0018032696076500155, 
             0.001713242874650142,   0.00060580255055524821, 0.00079434907915679187, 
             0.00051470524544852778,  0.0015023582571279484,  0.0012282224444965381, 
             3.3121306707951202e-05,  0.0019276211719760036,  0.0017020251617486965, 
             0.0020215166880727551,  0.00027546460398012482,  0.0018197588994445461, 
             0.0014621091688700844,  0.00034853183143781835, 0.00010723457520189531, 
             0.00057533230380395412, 0.00075071947472629631,  0.0001861507471649519, 
             0.0025665889628566545,   0.0024089581278189634,  0.0013911491057611431, 
             0.002964691989553305,    0.0019136566879232788,   0.001864510786405791, 
             0.00049587270459439407,  0.0021098477539105999,   0.004308478574088938, 
             0.0014918547318596622,   0.0011555145091341527,  0.0067268456981528363, 
             0.011304847669791245,    0.0096618283146946322, 2.7982665100048814e-05, 
             0.052377428770595605,    0.0098670846740628818,     1023.6621476225046, 
             0.10843897216272017,    0.00038963259518885127,   0.010512518087176327, 
             0.0026393340691132348,   0.0017500064404492475,  0.0013615765003283534, 
             0.0020024157733294331,  2.3650553402826256e-05,  0.0010891702293135911]
            )
          5
          0.95)))))

(deftest types-test
  (testing "Testing preprocessed data for the Lomb-Scargle algorithm"
    (is (= (count ls-freqs) keyboard-size))))