# -*- coding: utf-8 -*-
# markov.py
# author : Antoine Passemiers

import numpy as np


NO_TRANSITION = 0.005
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

BACH_MAJOR_PROBS = np.full((12, 12), NO_TRANSITION, dtype = np.float)
for i, x in enumerate([0, 2, 4, 5, 7, 9, 11]):
    for j, y in enumerate([0, 2, 4, 5, 7, 9, 11]):
        BACH_MAJOR_PROBS[x, y] = BACH_MAJOR[i, j]
BACH_MAJOR_PROBS[BACH_MAJOR_PROBS == .00] = NO_TRANSITION

BACH_MINOR_PROBS = np.full((12, 12), NO_TRANSITION, dtype = np.float)
for i, x in enumerate([0, 2, 3, 5, 7, 8, 10]):
    for j, y in enumerate([0, 2, 3, 5, 7, 8, 10]):
        BACH_MINOR_PROBS[x, y] = BACH_MINOR[i, j]
BACH_MINOR_PROBS[BACH_MINOR_PROBS == .00] = NO_TRANSITION


obs = np.array([6, 7, 7, 0, 0, 19, 12, 0, 22, 19, 12, 19, 19, 7, 19, 7, 0, 17, 10, 15, 14, 14, 5, 7, 5, 5, 0, 5, 12, 5, 3, 14, 19, 10, 12, 19, 19, 5, 10, 17, 3, 10, 5, 5, 0, 12, 4, 20, 18, 20, 18, 8, 11, 20, 20, 15, 8, 11, 11, 15, 11, 15, 4, 4, 20, 13, 6, 11, 14])
for i in range(len(obs)):
    if obs[i] >= 12:
        obs[i] -= 12

scores = np.zeros(12)

for k in range(12):
    loglikelihood = 0.0
    for i in range(1, len(obs)):
        a, b = (obs[i-1] + 12 - k) % 12, (obs[i] + 12 - k) % 12
        prob = BACH_MINOR_PROBS[a, b]
        if a != b:
            loglikelihood += np.log(prob)
        if obs[i] == k:
            loglikelihood += FUNDAMENTAL_SCORE

    index = k
    scores[index] = loglikelihood

print(scores)
print(np.argmax(scores) % 12)