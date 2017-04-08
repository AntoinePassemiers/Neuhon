# Neuhon

Mini-thesis on key signature detection, where both accuracy and efficiency of 
different algorithms are discussed. Python has been used for prototyping and research purposes only : the final end-user program is available in Clojure only.

## How to use Neuhon

```clj
    (use 'neuhon.core)

    ;; Predict the key of all files located in path/to/your/wave/folder
    (process-all "path/to/your/wave/folder")
```

## Clojure algorithms

- Comparing chromatic vectors and Krumhansl's tone profiles

### Clojure dependencies

- JTransforms
- clojure.core.matrix 0.58.0
- clojure.data.csv 0.1.3
- commons-io 2.5
- intervox/clj-progress 0.2.1

## Python algorithms

- Comparing chromatic vectors and Krumhansl's tone profiles
- Training Hidden Markov Models with chromatic vectors - TODO
- Training an Input-Output Hidden Markov Model with chromatic vectors

### Python dependencies

- Python 2.7
- Numpy (>= 1.6.1)
- Cython
- Theano
- StringIO
- ArchMM ( https://github.com/AntoinePassemiers/ArchMM )

## License

Copyright © 2016 Neuhon

Distributed under the Eclipse Public License either version 1.0.
