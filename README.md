# Neuhon

Mini-thesis on key detection, where 
different algorithms are discussed according to their accuracy and speed. 
Python has been used for prototyping and research purposes only : the final end-user program is available in Clojure only.
A Python version will be available soon.

## How to use it

Using Leiningen :

```sh
    $ lein repl
```

Simply put all the wav files you need in a folder and specify its path.
All the files will be processed in a single run. If you need to analyse
a single file, specify the filename.

```clj
    user=> (use 'neuhon.core)

    ;; Predict the key of all files located in path/to/your/wave/folder
    user=> (process-all "path/to/your/wave/folder")

    ;; Allow multithreading
    user=> (process-all 
                "path/to/your/wave/folder"
                :threading? true)

    ;; Sliding window with an overlap of 40% (default to 0.0) -> slower
    user=> (process-all 
                "path/to/your/wave/file"
                :threading? true
                :overlap 0.40)
				
    ;; Use the Direct Spectral Kernel Transform
    user=> (process-all 
                "path/to/your/wave/folder"
                :use-cqt? true)
				
    ;; Use the Lomb-Scargle least squares spectral estimation
    user=> (process-all 
                "path/to/your/wave/folder"
                :use-cqt? false)
```

### Testing

Using Leiningen :

```sh
    $ lein test
```

## Clojure

### Algorithm design

0) Pre-processing : Averaging the channels, downsampling
1) Overlaying data samples from chunks that are contiguous
2) Spectral density estimation with Lomb-Scargle method
3) Reshaping the Lomb-Scargle periodogram into a chromatic vector (12 elements)
4) Computing the correlation between chromatic vectors and custom tone profiles
5) Taking the tone profile that maximizes the Pearson correlation coefficient
6) Combine local predictions using Markov chains

### Clojure dependencies

- clojure.core.matrix 0.58.0
- clojure.data.csv 0.1.3
- commons-io 2.5
- intervox/clj-progress 0.2.1

## Python algorithms

Spectral density estimation :
- Fast Fourier Transform (FFT)
- Direct spectral kernel transform (DSKT)
- Vaníček periodogram
- Lomb-Scargle periodogram

Tone prediction :
- Comparing chromatic vectors and Krumhansl's tone profiles
- Comparing chromatic vectors and custom tone profiles

Tone prediction using machine learning :
- Hidden Markov Models and Baum-Welch algorithm
- Input-Output Hidden Markov Model and Generalized Expectation-maximization algorithm
- Classification trees

### Python dependencies

- Python 2.7
- Numpy (>= 1.6.1)
- Cython
- Theano
- StringIO
- Sci-kit learn
- ArchMM ( https://github.com/AntoinePassemiers/ArchMM )

## License

Copyright © 2016 Neuhon

Distributed under the Eclipse Public License either version 1.0.
