(defn jlog2 [n] (/ (Math/log n) (Math/log 2)))
	
;; (sample-player song-sample)
;; (def outseq (create-buffer-data) 1024 #(Math/sin %) 0 0)
;; code example : (FFT outseq (take song-sample 1024) 1024)
(def song-sample (load-sample "C://H Projects/Components/Uppermost - Born Limitless.wav"))

;; BIBLIOGRAPHIE
;; http://introcs.cs.princeton.edu/java/97data/FFT.java.html
;; http://introcs.cs.princeton.edu/java/97data/Complex.java.html

(deftype ComplexArray [real imag])
(deftype Complex [real imag])

(defn new-complex-array [N] 
	(ComplexArray. (make-array Double/TYPE N) (make-array Double/TYPE N)))

(defn get-complex [arr k]
	(Complex. (aget (.real arr) k) (aget (.imag arr) k)))

(defn set-complex [arr c k] 
	(do (aset (.real arr) k (.real c))
		(aset (.imag arr) k (.imag c))))

(defn complex-plus [c c2]
	(let [new_real (+ (.real c) (.real c2))
		  new_imag (+ (.imag c2) (.imag c2))]
		(Complex. new_real new_imag)))

(defn complex-minus [c c2]
	(let [new_real (- (.real c) (.real c2))
		  new_imag (- (.imag c2) (.imag c2))]
		(Complex. new_real new_imag)))

(defn complex-mult [c c2]
	(let [a_real   (.real c)
		  a_imag   (.imag c)
		  b_real   (.real c2)
		  b_imag   (.imag c2)
		  new_real (- (* a_real b_real) (* a_imag b_imag))
		  new_imag (+ (* a_real b_imag) (* a_imag b_real))]
		(Complex. new_real new_imag)))

(defn complex-from-angle [angle] 
	(Complex. (Math/cos angle) (Math/sin angle)))

(defn FFT 
	([signal N]
	(if-not (= (mod (jlog2 N) 1) 0.0)
		(throw (Exception. (str "Window size is not a power of 2")))
		(let [half (/ N 2)
			  in_real   (.real signal)
			  in_imag   (.imag signal)
			  even_arr  (new-complex-array half)
			  odd_arr   (new-complex-array half)]
		 	(if (= N 1) 
		 		(ComplexArray. (aget in_real 0) (aget in_imag 0))
		 		(do 
		 			(for [i (range half)] (do
		 				(aset (.real even_arr) i (aget in_real (* 2 i)))
		 				(aset (.imag even_arr) i (aget in_imag (* 2 i)))))
		 			(for [i (range half)] (do
		 				(aset (.real odd_arr) i (aget in_real (+ 1 (* 2 i))))
		 				(aset (.imag odd_arr) i (aget in_imag (+ 1 (* 2 i))))))
		 			(let [q (FFT even_arr half)
		 				  r (FFT odd_arr half)
		 				  result (new-complex-array N)]
		 				(do (for [i (range half)] (do (
		 						(let [wk (complex-from-angle (- (/ (* (* (2 Math/PI)) i) N)))
		 							  rk (complex-mult wk (get-complex r i))
		 							  ya (complex-plus (get-complex q i) rk)
		 							  yb (complex-minus (get-complex q i) rk)]
		 							(do (set-complex result i ya)
		 								(set-complex result (+ i half) yb)))))) 
		 					result
		 				)
		 			)
		 		)
		 	)
		)
	))
)