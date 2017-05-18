# -*- coding: utf-8 -*-
# markov.py
# author : Antoine Passemiers

import numpy as np
import pickle

from utils import KEY_NAMES, KEY_DICT

def rotateKey(key, shift):
    return (key + 24 - shift) % 24

def predictKeyWithArgmax(obs):
    return KEY_NAMES[np.argmax(np.histogram(obs, np.arange(25))[0])]

def predictKeyWithOneMatrix(obs, B):
    scores = np.zeros(24, dtype = np.double)
    for i in range(24):
        for t in range(1, len(obs)):
            x = rotateKey(obs[t - 1], i)
            y = rotateKey(obs[t], i)
            scores[i] += np.log2(B[x, y])
    return KEY_NAMES[scores.argmax()]

if __name__ == "__main__":
    B = np.zeros((24, 24), dtype = np.float)
    dataset = pickle.load(open("markov_dataset.npy", "rb"))
    for (observations, key) in dataset[:len(dataset)/2]:
        for i in range(1, len(observations)):
            k = KEY_DICT[key]
            x = rotateKey(observations[i - 1], k)
            y = rotateKey(observations[i], k)
            B[x, y] += 1
    for i in range(24):
        B[i, :] /= B[i, :].sum()
    tp = 0
    for (observations, key) in dataset[len(dataset)/2:]:
        pred = predictKeyWithOneMatrix(observations, B)
        if pred == key:
            tp += 1
    print(tp)
    print(list(B))
    