# -*- coding: utf-8 -*-
# markov.py
# author : Antoine Passemiers

import numpy as np


NO_TRANSITION = 0.001
FUNDAMENTAL_SCORE = np.log(1.00001)

BACH_MINOR = np.array(
    [[.00, .18, .01, .20, .41, .09, .12],
     [.01, .00, .03, .00, .89, .00, .07],
     [.06, .06, .00, .25, .19, .31, .13],
     [.22, .14, .00, .00, .48, .00, .15],
     [.80, .00, .02, .06, .00, .10, .02],
     [.03, .54, .03, .14, .19, .00, .08],
     [.81, .00, .01, .03, .15, .00, .00]])

BACH_MAJOR = np.array(
    [[.00, .15, .01, .28, .41, .09, .06],
     [.01, .00, .00, .00, .71, .01, .25],
     [.03, .03, .00, .52, .06, .32, .02],
     [.22, .13, .00, .00, .39, .02, .23],
     [.82, .01, .00, .07, .00, .09, .00],
     [.15, .29, .05, .11, .32, .00, .09],
     [.91, .00, .01, .02, .04, .03, .00]])

BACH_PROBS = np.full((24, 24), NO_TRANSITION, dtype = np.float)

for i, x in enumerate([0, 2, 4, 5, 7, 9, 11]):
    for j, y in enumerate([0, 2, 4, 5, 7, 9, 11]):
        BACH_PROBS[x, y] = BACH_MAJOR[i, j]

for i, x in enumerate([12, 14, 15, 17, 19, 20, 22]):
    for j, y in enumerate([12, 14, 15, 17, 19, 20, 22]):
        BACH_PROBS[x, y] = BACH_MINOR[i, j]

BACH_PROBS[BACH_PROBS == .00] = NO_TRANSITION


obs = np.array([21, 4, 4, 4, 21, 4, 12, 3, 7, 12, 3, 4, 8, 8, 20, 8, 8, 20, 13, 21, 20, 21, 0, 17, 14, 3, 14, 2, 3, 15, 10, 15, 14, 10, 15, 14, 3, 20, 10, 3, 15, 15, 14, 16, 14, 5, 15, 15, 17, 14, 14, 4, 23, 14, 14, 10, 14, 14, 10, 2, 14, 3, 15, 14, 3, 15, 14, 3, 3, 15, 10, 15, 10, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 22, 14, 10, 15, 15, 10, 3, 15, 10, 3, 14, 10, 23, 15, 14, 2, 15, 14, 10, 15, 2, 10, 15, 14, 10, 15, 15, 15, 15, 15, 3, 15, 2, 4, 7])

scores = np.zeros(24)

for k in range(24):
    loglikelihood = 0.0
    for i in range(1, len(obs)):
        a, b = (obs[i-1] + 24 - k) % 24, (obs[i] + 24 - k) % 24
        prob = BACH_PROBS[a, b]
        if a != b:
            loglikelihood += np.log(prob)
        if obs[i] == k:
            loglikelihood += FUNDAMENTAL_SCORE

    index = k
    scores[index] = loglikelihood

print(scores)
print(np.argmax(scores) % 24)