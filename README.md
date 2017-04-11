# Neuhon

Mini-thesis on key signature detection, where both accuracy and efficiency of 
different algorithms are discussed. Python has been used for prototyping and research purposes only : the final end-user program is available in Clojure only.

## How to use it

Simply put all the wav files you need in a folder and specify its path.
All the files will be processed in a single run :

```clj
    user=> (use 'neuhon.core)

    ;; Predict the key of all files located in path/to/your/wave/folder
    user=> (process-all "path/to/your/wave/folder")

    ;; Allow multithreading
    user=> (process-all "path/to/your/wave/folder"
                :threading true)
```

### Testing

Using Leiningen :

```sh
    $ lein test
```

## Clojure main algorithm :

1) Spectral density estimation with Lomb-Scargle method
2) Reshape the Lomb-Scargle periodogram to be a chromatic vector (12 elements)
3) Compute the correlation between chromatic vectors and custom tone profiles
4) Take the tone profile that maximizes the Pearson correlation coefficient

### Clojure dependencies

- JTransforms
- clojure.core.matrix 0.58.0
- clojure.data.csv 0.1.3
- commons-io 2.5
- intervox/clj-progress 0.2.1

## Python algorithms

Spectral density estimation :
- Fast Fourier Transform (FFT)
- Constant-Q Transform (CQT)
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
