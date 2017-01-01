(def major-coefs (to-array (seq [6.4 2.2 3.5 2.3 4.4 4.1 2.5 5.2 2.4 3.7 2.3 2.9])))
(def minor-coefs (to-array (seq [6.4 2.8 3.6 5.4 2.7 3.6 2.6 4.8 4.0 2.7 3.3 3.2])))

(deftype Profile [coefs fondamental])

(def major-key-profile (Profile. major-coefs 0))
(def minor-key-profile (Profile. minor-coefs 0))

(def profile-data-type Double/TYPE)

(defn substract-profiles [profile_A profile_B]
	(let [arr_A (.coefs profile_A)
		  arr_B (.coefs profile_B)
		  fondamental (.fondamental profile_A)
		  N (alength arr_A)
		  new_arr (make-array profile-data-type N)]
		(do (loop [i 0]
			(when (< i N)
				(aset new_arr i (- (aget arr_A i) (aget arr_B i)))
				(recur (inc i)))) (Profile. new_arr fondamental))))
