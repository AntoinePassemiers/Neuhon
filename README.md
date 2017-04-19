# Neuhon

Mini-thesis on key signature detection, where both accuracy and efficiency of 
different algorithms are discussed. Python has been used for prototyping and research purposes only : the final end-user program is available in Clojure only.
A Python version will be soon available.

## How to use it

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
```

### Testing

Using Leiningen :

```sh
    $ lein test
```

## Clojure

### Algorithm design

0) Pre-processing : Averaging the channels, downsampling
1) Spectral density estimation with Lomb-Scargle method
2) Reshaping the Lomb-Scargle periodogram into a chromatic vector (12 elements)
3) Computing the correlation between chromatic vectors and custom tone profiles
4) Taking the tone profile that maximizes the Pearson correlation coefficient
5) Combine local predictions using Markov chains

### TODO

- Use Markov chains to take the temporal factor into account
- Adapt the ML algorithms to be able to manage imbalanced datasets
- Handle wav files that are not 44100 Hz 2-channels

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
- TODO : Support vector machines

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
