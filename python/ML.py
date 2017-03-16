# -*- coding: utf-8 -*-
# ML.py : Key prediction using machine learning algorithms
# author: Antoine Passemiers

import os, operator, pickle
import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr

# https://github.com/AntoinePassemiers/ArchMM
from archmm.core import *
from archmm.iohmm import *

from utils import *
from prototype import *

# Key names of the 12 major scales and the 12 minor scales
KEY_NAMES = frozenset(("C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "Cm", "C#m",
    "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "G#m", "Am", "Bbm", "Bm"))
# Mapping between the key signature and its corresponding index in Numpy arrays
labels = { key : i for i, key in enumerate(KEY_NAMES) }
# Mapping between the key signature index and its corresponding name
key_names = { i : key for i, key in enumerate(KEY_NAMES) }
# Label counters
key_counters = { key : 0 for key in KEY_NAMES }

# Parameters for Input-Output Hidden Markov Models
config = IOConfig()
config.architecture = "ergodic"  # Linear topology
config.n_iterations = 30         # Number of iterations of the GEM
config.s_learning_rate  = 0.02   # Learning rate of the initial state unit
config.o_learning_rate  = 0.02   # Learning rate of the state transition units
config.pi_learning_rate = 0.02   # Learning rate of the output units
config.missing_value_sym = np.nan_to_num(np.nan) # Default missing value
config.pi_activation = "sigmoid" # Activation function of the initial state unit
config.s_activation  = "sigmoid" # Activation function of the state transition units
config.o_activation  = "sigmoid" # Activation function of the output units
config.pi_nhidden = 25 # Number of hidden neurons in the initial state unit
config.s_nhidden  = 25 # Number of hidden neurons in the state transition units
config.o_nhidden  = 25 # Number of hidden neurons in the output units
config.pi_nepochs = 4  # Number of training epochs per iteration for the initial state unit
config.s_nepochs  = 4  # Number of training epochs per iteration for the state transition units
config.o_nepochs  = 4  # Number of training epochs per iteration for the output units

config.use_chromatic_vector = False # Use chromatic features or FFT coefficients

def extractCQTs(filename):
    """ Loading wav file """
    stereo_signal = getSignalFromFile(filename)
    """ Averaging the 2 channels (stereo -> mono) """
    signal = stereoToMono(stereo_signal) # Mean of left and right channels
    """ Low-pass filtering """
    signal = lowPassFiltering(signal)
    """ Downsampling """
    signal = downSampling(signal)
    """ Computing Short-Term Energies and Zero Crossing Rates """
    ste_sequence, zcr_sequence = getSTEandZCRs(signal)
    """ Computing real Fast Fourier Transforms """
    fft_matrix = getFFTs(signal)
    """ Spectral windows for getting CQT from real spectrum """
    wins = getSpectralWindows(len(signal))
    """ Computing Constant-Q Transforms """
    cqt_matrix = getCQTs(fft_matrix, wins)
    return ste_sequence, cqt_matrix

def extractFeatures(dataset):
    inputs, targets = list(), list()
    for i, entry in enumerate(dataset):
        target_key, filename = entry[2], entry[3]
        try:
            print("Processing file %i" % i)
            if config.use_chromatic_vector:
                _, feature_matrix = findKey(filename)
            else:
                _, feature_matrix = extractCQTs(filename)
            inputs.append(feature_matrix)
            targets.append(np.full((len(feature_matrix),), labels[target_key], dtype = np.int32))
        except IOError:
            pass
    pickle.dump([inputs, targets], open("temp", "wb"))

def train():
    temps = pickle.load(open("temp", "rb"))
    X, targets = temps[0], temps[1]
    print(len(X), X[0].shape)
    for target in targets:
        key_counters[key_names[target[0]]] += 1
    print(key_counters)
    iohmm = HMM(5, has_io = True, standardize = False)
    iohmm.fit(X, targets = targets, n_classes = 24, is_classifier = True, parameters = config)
    iohmm.pySave("model")
    return iohmm

def predict(dataset):
    model = HMM(5, has_io = True, standardize = False)
    model.pyLoad("model")

    tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0
    distances = np.zeros(12)
    for i, entry in enumerate(dataset):
        artist, title, target_key, filename = entry[0], entry[1], entry[2], entry[3]
        try:
            if config.use_chromatic_vector:
                _, feature_matrix = findKey(filename)
            else:
                _, feature_matrix = extractCQTs(filename)
            result = model.predictIO(feature_matrix)
            predicted_key = key_names[result[0]]

            n_total += 1

            if predicted_key == target_key:
                tp += 1
            elif isParallel(predicted_key, target_key):
                parallels += 1
            elif isRelative(predicted_key, target_key):
                relatives += 1
            elif isOutByAFifth(predicted_key, target_key):
                out_by_a_fifth += 1
            else:
                fp += 1
        except IOError:
            pass

        showWavFileResults(i, artist, title, target_key, predicted_key)
    showFinalResults(tp, out_by_a_fifth, parallels, relatives, fp, n_total)
    print("Finished")

def splitDataset(n_files, split_proportion = 0.5):
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()

    indexes = np.arange(n_files)
    np.random.shuffle(indexes)
    split_index = int(np.round(n_files * split_proportion))
    training_indexes = indexes[:split_index]
    test_indexes = indexes[split_index:]

    training_set, test_set = list(), list()
    for i in range(n_files):
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]
        if i in training_indexes:
            training_set.append([artist, title, target_key, filename])
        else:
            test_set.append([artist, title, target_key, filename])
    csv_file.close()
    return training_set, test_set

if __name__ == "__main__":
    np.random.seed(0)
    training_set, test_set = splitDataset(230, split_proportion = 0.5)
    
    # extractFeatures(training_set)
    model = train()
    predict(test_set)