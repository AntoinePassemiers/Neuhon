# Neuhon

A Clojure software for key signature detection in musical audio files

## Usage

```clj
    ;; boot the server
    user=> (use 'overtone.live)

    ;; listen to the joys of a simple sine wave
    user=> (demo (sin-osc))

    ;; or something more interesting...
    user=>(demo 7 (lpf (mix (saw [50 (line 100 1600 5) 101 100.5]))
                  (lin-lin (lf-tri (line 2 20 5)) -1 1 400 4000)))
```

## License

Copyright © 2016 Neuhon

Distributed under the Eclipse Public License either version 1.0.
