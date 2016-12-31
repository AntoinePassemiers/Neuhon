(def win-coef-type Double/TYPE)

(deftype Window [coefs lk rk])

(defn get-Q-from-p [p]
	(* p (- (long (Math/pow 2 (/ 1.0 12))) 1)))

(defn win-left-bound [Q fk N sampling-rate] 
	(double (Math/round (* (- 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn win-right-bound [Q fk N sampling-rate] 
	(double (Math/round (* (+ 1 (/ (double Q) 2)) (/ (double (* fk N)) (double sampling-rate))))))

(defn cosine-win-element [x lk rk]
	(let [relative-pos (/ (double (- x lk)) (- rk lk))]
		(- 1.0 (Math/cos (* 2 (double (* Math/PI relative-pos)))))))

(defn cosine-win [Q fk N sampling-rate]
	(let [lk (win-left-bound Q fk N sampling-rate)
		  rk (win-right-bound Q fk N sampling-rate)
		  win-size (- rk lk)
		  win (make-array win-coef-type win-size)]
		(do (loop [i 0]
			(when (< i win-size)
				(aset win i (cosine-win-element (+ lk i) lk rk))
				(recur (inc i))))
			(Window. win lk rk))))

(defn cqt-coefficient [])