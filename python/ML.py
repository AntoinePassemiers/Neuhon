# -*- coding: utf-8 -*-
# ML.py : Key prediction using machine learning algorithms
# author: Antoine Passemiers

import os, operator, pickle, sys
import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr

# https://github.com/AntoinePassemiers/ArchMM
from archmm.core import *
from archmm.iohmm import *

from utils import *
from cognitive import *
from spectral import *

FFT_METHOD = 0
CQT_METHOD = 1
LOMB_SCARGLE_METHOD = 2

# Key names of the 12 major scales and the 12 minor scales
KEY_NAMES = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B", "Cm", "C#m",
    "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "G#m", "Am", "Bbm", "Bm"]
# Mapping between the key signature and its corresponding index in Numpy arrays
labels = { key : i for i, key in enumerate(KEY_NAMES) }
# Mapping between the key signature index and its corresponding name
key_names = { i : key for i, key in enumerate(KEY_NAMES) }
# Label counters
key_counters = { key : 0 for key in KEY_NAMES }

# Parameters for Input-Output Hidden Markov Models
config = IOConfig()
config.architecture = "linear"  # Ergodic topology
config.n_iterations = 25         # Number of iterations of the GEM
config.s_learning_rate  = 0.03   # Learning rate of the initial state unit
config.o_learning_rate  = 0.03   # Learning rate of the state transition units
config.pi_learning_rate = 0.03   # Learning rate of the output units
config.missing_value_sym = np.nan_to_num(np.nan) # Default missing value
config.pi_activation = "sigmoid" # Activation function of the initial state unit
config.s_activation  = "tanh" # Activation function of the state transition units
config.o_activation  = "tanh" # Activation function of the output units
config.pi_nhidden = 20 # Number of hidden neurons in the initial state unit
config.s_nhidden  = 20 # Number of hidden neurons in the state transition units
config.o_nhidden  = 30 # Number of hidden neurons in the output units
config.pi_nepochs = 1  # Number of training epochs per iteration for the initial state unit
config.s_nepochs  = 1  # Number of training epochs per iteration for the state transition units
config.o_nepochs  = 1  # Number of training epochs per iteration for the output units

config.use = LOMB_SCARGLE_METHOD

TRAINING_SET = np.array([
    45,  51,  75,  126, 146, 167, # C
    # C#
    108, 108, 108, 108, 108, 108, # D
    276, 276, 276, 276, 276, 276, # Eb
    56,  149, 56,  149, 56,  149, # E
    39,  79,  143, 168, 39,  79,  # F
    150, 150, 150, 150, 150, 150, # F#
    14,  14,  14,  14,  14,  14,  # G
    40,  40,  40,  40,  40,  40,  # G#
    7,   30,  84,  7,   30,  84,  # A
    49,  133, 207, 49,  133, 207, # Bb
    183, 237, 183, 237, 183, 237, # B
    20,  34,  46,  50,  86,  101, # Cm
    5,   69,  77,  170, 225, 5,   # C#m
    12,  17,  28,  64,  70,  91,  # Dm
    3,   6,   9,   25,  26,  41,  # Ebm
    8,   11,  13,  33,  35,  55,  # Em
    19,  22,  27,  31,  42,  43,  # Fm
    21,  48,  82,  88,  89,  93,  # F#m
    2,   23,  54,  83,  111, 116, # Gm
    15,  18,  73,  97,  115, 129, # G#m
    4,   16,  29,  32,  36,  37,  # Am
    10,  24,  52,  132, 134, 148, # Bbm
    44,  68,  85,  87,  94,  98,  # Bm
])

def extractFeatures(filename):
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
    """ Computing the spectra """
    feature_matrix = getPeriodograms(signal)

    
    # wins = getSpectralWindows(len(signal))
    # cqt_matrix = getCQTs(fft_matrix, wins)
    
    return feature_matrix

def saveFeatures(dataset):
    inputs, targets = dict(), dict()
    for i in dataset.keys():
        entry = dataset[i]
        target_key, filename = entry[2], entry[3]
        try:
            print("Processing file %i" % i)
            feature_matrix = extractFeatures(filename)
            inputs[i] = feature_matrix
            targets[i] = np.full((len(feature_matrix),), labels[target_key], dtype = np.int32)
        except IOError:
            pass
    pickle.dump([inputs, targets], open("temp", "wb"))

def train():
    temps = pickle.load(open("temp", "rb"))
    X, targets = temps[0], temps[1]
    train_X, train_y = list(), list()
    for i in TRAINING_SET:
        try:
            print(targets[i][0], i)
            train_X.append(X[i])
            train_y.append(targets[i])
        except KeyError:
            pass
    for i in range(len(train_y)):
        key_counters[key_names[train_y[i][0]]] += 1
    print(key_counters)

    iohmm = HMM(5, has_io = True, standardize = False)
    iohmm.fit(train_X, targets = train_y, n_classes = 24, is_classifier = True, parameters = config)
    iohmm.pySave("model")
    return iohmm

def predict():
    model = HMM(5, has_io = True, standardize = False)
    model.pyLoad("model")

    temps = pickle.load(open("temp", "rb"))
    X, targets = temps[0], temps[1]
    validation_X, validation_y = list(), list()
    for i in range(490):
        if i not in TRAINING_SET:        
            try:
                validation_X.append(X[i])
                validation_y.append(targets[i])
            except KeyError:
                pass
    tp, fp, relatives, parallels, out_by_a_fifth, out_by_a_fourth, n_total = 0, 0, 0, 0, 0, 0, 0
    distances = np.zeros(12)
    for i, entry in enumerate(validation_X):
        try:
            result = model.predictIO(validation_X[i])
            predicted_key = key_names[result[0]]
            target_key = key_names[validation_y[i][0]]
            n_total += 1

            print(predicted_key, target_key)
            if predicted_key == target_key:
                tp += 1
            elif isParallel(predicted_key, target_key):
                parallels += 1
            elif isRelative(predicted_key, target_key):
                relatives += 1
            elif isOutByAFifth(predicted_key, target_key):
                out_by_a_fifth += 1
            elif isOutByAFourth(predicted_key, target_key):
                out_by_a_fourth += 1
            else:
                fp += 1
        except IOError:
            pass

    showFinalResults(tp, out_by_a_fifth, out_by_a_fourth, parallels, relatives, fp, n_total)
    print("Finished")

def loadDataset(n_files, split_proportion = 0.5):
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()
    dataset = dict()
    for i in range(n_files):
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]
        dataset[i + 2] = [artist, title, target_key, filename]
    csv_file.close()
    return dataset

if __name__ == "__main__":
    #dataset = loadDataset(490, split_proportion = 0.5)
    #saveFeatures(dataset)
    
    # train()
    predict()