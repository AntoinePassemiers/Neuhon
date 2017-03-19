# Neuhon

Mini-thesis on key signature detection, where both accuracy and efficiency of 
different algorithms are discussed. Some of the latter are implemented in Clojure,
and some are written in Python.

## Clojure algorithms

- Comparing chromatic vectors and Krumhansl's tone profiles (using Constant-Q Transform)
- Comparing chromatic vectors and Krumhansl's tone profiles (using Average Magnitude Difference Function) - TODO

### Clojure dependencies

- JTransforms
- clojure.core.matrix
- clojure.data.csv
- commons-io

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
