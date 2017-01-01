(defn log2 [n] (/ (Math/log n) (Math/log 2)))
	
;; (def song-sample (load-sample "C://H Projects/Prosody/samples/ahhcut.wav"))
(def song-sample (load-sample "C://H Projects/Components/Uppermost - Born Limitless.wav" :start 400000))

;; (def song-buffer (buffer-alloc-read "C://H Projects/Components/Uppermost - Born Limitless.wav" 0 1024))
;; (buffer-get song-sample 15745)
;; (sample-player song-sample)


(use 'vizard.core)
(require '[vizard [core :refer :all] [plot :as plot] [lite :as lite]])
;; (start-plot-server!)
(defn plot-buffer
	([arr]
		(for [x (range (count arr))]
			{:x x :y (get arr x)}))
	([buf begin offset]
		(let [list (buffer-read buf begin offset)]
			(for [x (range (count list))]
				{:x x :y (get list x)})))
)
;; (plot! (plot/vizard {:mark-type :line} (plot-buffer buffer begin end)))

;; BIBLIOGRAPHIE
;; http://introcs.cs.princeton.edu/java/97data/FFT.java.html
;; http://introcs.cs.princeton.edu/java/97data/Complex.java.html

;; (complex-to-real (FFT (new-complex-array (buffer-read song-sample 0 1024) 1024) 1024))

;; http://asymmetrical-view.com/2009/07/02/clojure-primitive-arrays.html
;; https://equilibriumofnothing.wordpress.com/2013/10/14/algorithm-iterative-fft/

(set! *warn-on-reflection* true)
(set! *unchecked-math* true)

(def data-type Double/TYPE)

(deftype ComplexArray [^doubles real ^doubles imag])
(deftype Complex [^doubles real ^doubles imag])

(defn new-complex-array 
	([N] 
		(ComplexArray. (make-array data-type N) (make-array data-type N)))
	([^doubles signal N] 
		(ComplexArray. (into-array data-type signal) (make-array data-type N))))

(defn complex-to-real [arr]
	(let [N (alength (.real arr))
		  new_arr (make-array data-type N)]
		(do (loop [i 0]
			(when (< i N)
				(aset new_arr i (Math/sqrt 
					(+ (Math/pow (aget (.real arr) i) 2) 
						(Math/pow (aget (.imag arr) i) 2))))
				(recur (inc i))))
			new_arr)))

(defn get-complex [arr k]
	(Complex. (aget (.real arr) k) (aget (.imag arr) k)))

(defn set-complex 
	([arr k source kk]
		(do (aset (.real arr) k (aget (.real source) kk))
			(aset (.imag arr) k (aget (.imag source) kk))))
	([arr k c] 
	(do (aset (.real arr) k (.real c))
		(aset (.imag arr) k (.imag c)))))

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

(defn argmax 
	([arr] (argmax arr 0 (alength arr) Double/MIN_VALUE 0))
	([arr begin end] (argmax arr begin end Double/MIN_VALUE 0))
	([arr begin max-value best-index] 
	(do (if (= (alength arr) begin)
		best-index
		(if (> (aget arr begin) max-value)
			(argmax arr (+ begin 1) (aget arr begin) begin)
			(argmax arr (+ begin 1) max-value best-index))))))

(defn FFT 
	([signal N]
	(if-not (= (mod (log2 N) 1) 0.0)
		(throw (Exception. (str "Window size is not a power of 2")))
		(let [half      (/ N 2)
			  in_real   (.real signal)
			  in_imag   (.imag signal)
			  even_arr  (new-complex-array half)
			  odd_arr   (new-complex-array half)]
		 	(if (= N 1)
		 		(let [real_part (make-array data-type 1)
		 			  imag_part (make-array data-type 1)]
		 			(do 
		 				(aset real_part 0 (aget in_real 0))
		 				(aset imag_part 0 (aget in_imag 0))
		 				(ComplexArray. real_part imag_part)))
		 		(do 
		 			(loop [i 0]
		 				(when (< i half) (do
			 				(aset (.real even_arr) i (double (aget in_real (* 2 i))))
			 				(aset (.imag even_arr) i (double (aget in_imag (* 2 i))))
			 				(aset (.real odd_arr) i (double (aget in_real (+ 1 (* 2 i)))))
			 				(aset (.imag odd_arr) i (double (aget in_imag (+ 1 (* 2 i))))))
			 				(recur (inc i))))
		 			(let [q (FFT even_arr half)
		 				  r (FFT odd_arr half)
		 				  result (new-complex-array N)]
		 				(do (loop [i 0] 
	 						(when (< i half) (do
		 						(let [wk (complex-from-angle (- (/ (* (* 2 Math/PI) i) N)))
		 							  rk (complex-mult wk (get-complex r i))
		 							  ya (complex-plus (get-complex q i) rk)
		 							  yb (complex-minus (get-complex q i) rk)]
		 							(do (set-complex result i ya)
		 								(set-complex result (+ i half) yb))))
		 					(recur (inc i))))
		 					result))))))))


(defn reverse-bits 
	([k]
		(let [k-1sp (bit-or (bit-shift-left (bit-and k 0x55555555) 1) 
							(bit-and (bit-shift-right k 1) 0x55555555))
			  k-2sp (bit-or (bit-shift-left (bit-and k-1sp 0x33333333) 2) 
							(bit-and (bit-shift-right k-1sp 2) 0x33333333))
			  k-4sp (bit-or (bit-shift-left (bit-and k-2sp 0x0f0f0f0f) 4) 
							(bit-and (bit-shift-right k-2sp 4) 0x0f0f0f0f))
			  k-8sp (bit-or (bit-shift-left (bit-and k-4sp 0x00ff00ff) 8) 
							(bit-and (bit-shift-right k-4sp 8) 0x00ff00ff))
			  k-16sp (bit-or (bit-shift-left k-8sp 16) 
							(bit-shift-right k-8sp 16) 0x00ff00ff)]
			k-16sp)))

(defn iterativeFFT
	([signal]
		(let [N (alength signal)
			  nbits (log2 N)
			  toinverse (< nbits 0)
			  abs-nbits (Math/abs nbits)
			  result (make-array )] (do 
			(loop [i 0]
				(when (< i N) 
					(set-complex result i signal 
						(bit-shift-right (reverse-bits i) (- 32 abs-nbits)))
					(recur (inc i))))
      (loop [p 1]
				(when (<= p abs-nbits)
					(let [step (bit-shift-left 0x1 p)
						  theta (* 2 (* Math/PI (if toinverse -1 1)))
						  wk (complex-from-angle theta)]
						  (loop [offset 0]
						  	(when (< offset N))))
					(recur (inc p))))))))