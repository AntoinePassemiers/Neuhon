# -*- coding: utf-8 -*-
# cognitive.py
# author : Antoine Passemiers

import os, operator, pickle
import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr
from scipy.spatial.distance import cosine as cosine_similarity

from bontempo import *
from utils import *
from spectral import *

CSV_PATH = "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"
WAV_PATH = "D://KeyFinderDB"


METHOD_CQT          = 0xA86F20
METHOD_LOMB_SCARGLE = 0xA86F21

def createProfileMatrix(profile):
    mat = np.empty((12, 12), dtype = np.float)
    for i in range(0, 12):
        mat[i, :] = np.roll(profile, i)
    return mat

KRUMHANSL_MAJOR_BASE_PROFILE = np.array([6.4, 2.2, 3.5, 2.3, 4.4, 4.1, 2.5, 5.2, 2.4, 3.7, 2.3, 2.9])
KRUMHANSL_MINOR_BASE_PROFILE = np.array([6.4, 2.8, 3.6, 5.4, 2.7, 3.6, 2.6, 4.8, 4.0, 2.7, 3.3, 3.2])
SHAATH_MAJOR_BASE_PROFILE    = np.array([6.6, 2.0, 3.5, 2.2, 4.6, 4.0, 2.5, 5.2, 2.4, 3.8, 2.3, 3.4])
SHAATH_MINOR_BASE_PROFILE    = np.array([6.5, 2.8, 3.5, 5.4, 2.7, 3.5, 2.5, 5.1, 4.0, 2.7, 4.3, 3.2])

CUSTOM_MAJOR_BASE_PROFILE    = np.array(
    [ 8.89868836,  2.46479067,  4.03193409,  7.02790546,  4.55733136,  3.29152325,
     -0.15983709,  5.96139388,  3.40141878, -0.2675988,   4.10326596,  5.65157946])
CUSTOM_MINOR_BASE_PROFILE    = np.array(
    [ 6.63347485,  3.14954925,  2.94053816,  3.571383,    4.73633031,  4.90456669,
      2.81810968,  6.45527264,  0.56848472,  5.25402483,  1.7791656,   4.63136019])

CUSTOM_MAJOR_BASE_PROFILE_4096 = np.array(
    [ 8.65320429,  2.16161292,  4.30865426,  6.90719602,  4.3678239,  2.85374645,
     -0.53883001,  6.44311905,  3.38053384, -0.02761278,  3.76937104, 5.52525848])
CUSTOM_MINOR_BASE_PROFILE_4096 = np.array(
    [ 6.4305449,   3.26399386,  2.96707701,  3.93445561,  4.55335791,  5.25911852,
      2.9317162,   6.13092016,  0.73633773,  4.92132766,  2.10301337,  4.67029709])

CUSTOM_MAJOR_BASE_PROFILE_8192 = np.array(
    [ 9.1216041,   1.87377359,  4.54028516,  6.29457256,  3.37665931,  3.44749752,
      0.06987374,  5.62058805,  3.85578954, -0.77826889,  4.37297711,  5.00763079])
CUSTOM_MINOR_BASE_PROFILE_8192 = np.array(
    [ 6.52646718,  3.33285153,  3.36638347,  3.99483838,  4.76466513,  5.85239725,
      2.96574636,  5.45032911,  0.22028792,  4.55233485,  2.01474569,  4.21566567])

CUSTOM_MAJOR_BASE_PROFILE_12288 = np.array(
    [ 8.99168964,  1.81071541,  4.44105919,  7.04221016,  3.83114769,  3.42865417,
     -0.76578838,  6.30715069,  4.24797295, -0.86639359,  4.2135304,   5.22195534])
CUSTOM_MINOR_BASE_PROFILE_12288 = np.array(
    [ 6.27968744,  2.36048225,  2.99648225,  3.86702985,  5.55842947,  4.29812855,
      2.81645625,  6.01846971,  1.03219647,  5.31454025,  1.6183446,   4.33754876])

CUSTOM_MAJOR_BASE_PROFILE_16384 = np.array(
    [ 9.76099503,  1.76680785,  3.55390977,  7.68233716,  4.05116658,  2.75528644,
      0.18196855,  6.68043946,  3.43721581, -0.36641898,  3.75544749,  5.47563027])
CUSTOM_MINOR_BASE_PROFILE_16384 = np.array(
    [ 6.00835116,  3.2748862,   3.47497438,  3.16457523,  5.20081744,  5.04711892,
      3.00172431,  6.39331657,  0.39891709,  5.18524393,  1.12219695,  4.5121723 ])

CUSTOM_MAJOR_BASE_PROFILE_4096_OVERLAP2 = np.array(
    [  9.28376660e+00,   4.74244626e+00,   4.68856758e+00,   6.46091384e+00,
       4.91450998e+00,   3.56101998e+00,  -4.25175873e-03,   7.76621866e+00,
       4.41611987e+00,   5.84291796e-01,   6.00089530e+00,   3.02445090e+00])
CUSTOM_MINOR_BASE_PROFILE_4096_OVERLAP2 = np.array(
    [ 3.97941074,  2.83186428,  3.47202171,  4.60659078,  2.93005446,  5.63382232,
      2.63924404,  6.49747473, -0.78941772,  5.35740125,  1.35922185,  5.05482862])

CUSTOM_MAJOR_BASE_PROFILE_4096_CQT = np.array(
    [ 10.00643334,   4.13811934,   5.35888415,   6.56697766,   5.20696616,
       3.5933301,   -0.01040525,   7.64893975,   4.38476923,   0.01892959,
       7.28812808,   2.52406253])
CUSTOM_MINOR_BASE_PROFILE_4096_CQT = np.array(
    [ 3.54340645,  3.02966769,  3.4538301,   5.65823449,  2.9327288,   4.92434278,
      2.52811186,  5.88819169, -0.40295669,  4.90074726,  0.36529196,  5.10784805])

# CQT 4096 : (203, 18, 10, 56, 21, 107)
# 4096 + overlap2 : (194, 14, 11, 57, 32, 107)
# 4096 :  
# 8192 :  (154, 18, 10, 55, 40, 138) -> 0.371
# 12288 : (149, 13, 12, 60, 38, 143) -> 0.359
# 16384 : (152, 16, 9, 61, 39, 138)

CUSTOM_MAJOR_BASE_PROFILE = CUSTOM_MAJOR_BASE_PROFILE_4096_CQT
CUSTOM_MINOR_BASE_PROFILE = CUSTOM_MINOR_BASE_PROFILE_4096_CQT

MAJOR_PROFILE_MATRIX = createProfileMatrix(CUSTOM_MAJOR_BASE_PROFILE)
MINOR_PROFILE_MATRIX = createProfileMatrix(CUSTOM_MINOR_BASE_PROFILE)

class ExtraFeatures:
    def __init__(self):
        self.ZCR = None
        self.STE = None

def w_xk(x, lk, rk):
    return 1.0 - np.cos(2 * np.pi * (x - lk) / (rk - lk))

def QFromP(p):
    return p * (np.power(2.0, 1.0 / 12.0) - 1.0)

def winBounds(Q, fk, N, sampling_rate):
    rang = float(fk * N) / float(sampling_rate)
    lk = (1 - Q / 2.0) * rang
    rk = (1 + Q / 2.0) * rang
    return lk, rk

def findNearestBoundsInSpectrum(fft_freqs, lk, rk):
    li = (np.abs(fft_freqs - lk)).argmin()
    ri = (np.abs(fft_freqs - rk)).argmin()
    if li == ri:
        ri += 1
    return li, ri

def getSignalFromFile(filename):
    filepath = os.path.join(WAV_PATH, filename)
    framerate, signal = scipy_read(filepath)
    assert(framerate == Parameters.sampling_rate)
    assert(signal.shape[1] == Parameters.n_channels)
    return signal

def stereoToMono(signal):
    if len(signal.shape) == 1 or signal.shape[1] == 1:
        return signal
    else:
        return signal.mean(axis = 1)

def moving_average(signal, n = 15):
    return np.convolve(signal, np.ones((n,)) / n, mode = "valid")

def butter_lowpass_filter(data, cutoff, fs, order = Parameters.lowpass_filter_order):
    nyquist = 0.5 * fs
    normal_cutoff = cutoff / nyquist
    b, a = butter(order, normal_cutoff, btype = 'low', analog = False)
    return lfilter(b, a, data)

def lowPassFiltering(signal):
    signal = butter_lowpass_filter(
        signal, 
        Parameters.lowpass_filter_cutoff_freq, 
        Parameters.sampling_rate
    ) # Low-pass filtering # TODO : Fisher's filter
    return signal

def downSampling(signal, framerate = 4410.0):
    indexes = np.asarray(np.arange(
        0, len(signal), 
        float(Parameters.sampling_rate) / framerate
    ), dtype = np.int)
    signal = signal[indexes]
    return signal

def getSpectralWindows(framerate = 4410.0):
    assert(Parameters.n_octaves == (Parameters.max_midi_note - Parameters.min_midi_note) / 12)
    assert((Parameters.max_midi_note - Parameters.min_midi_note) % 12 == 0)
    fft_freqs = np.fft.fftfreq(Parameters.window_size) * framerate
    Q = QFromP(0.8)
    wins = list()
    for fk in midiToHertz(np.arange(Parameters.min_midi_note, Parameters.max_midi_note)):
        lk, rk = winBounds(Q, fk, Parameters.window_size, framerate)
        li, ri = findNearestBoundsInSpectrum(fft_freqs, lk, rk)
        win = w_xk(fft_freqs[li:ri+1], lk, rk)
        win /= win.sum()
        wins.append((li, ri, win))
    return wins

def getSTEandZCRs(signal):
    i, n_vectors = 0, 0
    n_samples = len(signal)
    T = n_samples - Parameters.window_size
    blackman_win = np.blackman(Parameters.window_size)
    ste_sequence = np.empty(T / Parameters.window_size + 1, dtype = np.double)
    zcr_sequence = np.empty(T / Parameters.window_size + 1, dtype = np.double)
    while i < T:
        frame = blackman_win * signal[i:i+Parameters.window_size]
        ste_sequence[n_vectors] = (frame ** 2).sum()
        zcr_sequence[n_vectors] = len(np.where(np.diff(np.signbit(signal)))[0])
        i += Parameters.window_size
        n_vectors += 1
    return ste_sequence, zcr_sequence

def getFFTs(signal, ticks = None):
    i, n_vectors = 0, 0
    n_coefs = Parameters.window_size
    n_samples = len(signal)
    T = n_samples - Parameters.window_size
    blackman_win = np.blackman(Parameters.window_size)
    if ticks is None:
        fft_matrix = np.empty((T / Parameters.window_size + 1, n_coefs), dtype = np.double)
        while i < T:
            frame = blackman_win * signal[i:i+Parameters.window_size]
            i += Parameters.window_size
            fft_matrix[n_vectors, :] = np.abs(np.fft.fft(frame))
            n_vectors += 1
    else:
        fft_matrix = np.empty((len(ticks), n_coefs), dtype = np.double)
        for tick in ticks:
            try:
                frame = blackman_win * signal[tick:tick+Parameters.window_size]
            except ValueError:
                pass
            fft_matrix[n_vectors, :] = np.abs(np.fft.fft(frame))
            n_vectors += 1
    return fft_matrix

def getCQTs(fft_matrix, wins):
    n_vectors = 0
    n_coefs = len(wins)
    cqt_matrix = np.empty((len(fft_matrix), n_coefs), dtype = np.double)
    while n_vectors < len(fft_matrix):
        for k in range(len(wins)):
            li, ri, win = wins[k]
            cqt_matrix[n_vectors, k] = (win * fft_matrix[n_vectors, li:ri+1]).sum()
        n_vectors += 1
    return cqt_matrix

def predictKeyFromHistogram(hist):
    kk = np.argmax(hist)
    predicted_key_name = KEY_NAMES[kk]
    return predicted_key_name

def matchWithProfiles(coefs, major_profile_matrix, minor_profile_matrix):
    major_scores, minor_scores = np.zeros(12), np.zeros(12)
    for j in range(12):
        major_scores[j] = pearsonr(coefs, major_profile_matrix[j])[0]
        minor_scores[j] = pearsonr(coefs, minor_profile_matrix[j])[0]


    best_major_key = major_scores.argmax()
    best_minor_key = minor_scores.argmax()
    if major_scores[best_major_key] > minor_scores[best_minor_key]:
        kk = (Parameters.min_midi_note - 1 + best_major_key) % 12 + 12
    else:
        kk = (Parameters.min_midi_note - 1 + best_minor_key) % 12
    return kk

def findKey(filename, method = METHOD_CQT):
    hist = np.zeros(24, dtype = np.int)
    """ Loading wav file """
    stereo_signal = getSignalFromFile(filename)
    """ Averaging the 2 channels (stereo -> mono) """
    signal = stereoToMono(stereo_signal) # Mean of left and right channels
    """ Low-pass filtering """
    # signal = lowPassFiltering(signal)
    # signal = moving_average(signal)
    """ Downsampling """
    signal = downSampling(signal, framerate = Parameters.target_sampling_rate)
    """ Spectral windows for getting CQT from real spectrum """
    wins = getSpectralWindows(framerate = Parameters.target_sampling_rate)
    """ Computing Short-Term Energies """
    # ste_sequence, zcr_sequence = getSTEandZCRs(signal)
    extra_features = ExtraFeatures()
    # extra_features.STE = ste_sequence
    # extra_features.ZCR = zcr_sequence

    chromatic_matrices = list()
    if method == METHOD_CQT:
        """ Computing real Fast Fourier Transforms """
        fft_matrix = getFFTs(signal)
        """ Computing Constant-Q Transforms """
        feature_matrix = getCQTs(fft_matrix, wins)
    elif method == METHOD_LOMB_SCARGLE:
        """ Computing Lomb-Scargle periodograms """
        feature_matrix = getPeriodograms(signal)
        # print(list(feature_matrix[30]))

    obs_seq = list()
    n_samples = len(feature_matrix)
    chromatic_matrix = np.empty((n_samples, 12), dtype = np.double)
    
    n_vectors = 0
    while n_vectors < n_samples:
        coefs = feature_matrix[n_vectors]
        coefs = np.reshape(coefs, (Parameters.n_octaves, 12))
        p = Parameters.chromatic_max_weight
        coefs = p * coefs.max(axis = 0) + (1.0 - p) * coefs.sum(axis = 0)
        chromatic_matrix[n_vectors, :] = coefs[:]

        kk = matchWithProfiles(coefs, MAJOR_PROFILE_MATRIX, MINOR_PROFILE_MATRIX)
        obs_seq.append(kk)
        hist[kk] += 1
        n_vectors += 1
    print(obs_seq)
    extra_features.obs_seq = obs_seq
    print(hist.reshape(2, 12))
    predicted_key_name = predictKeyFromHistogram(hist)
    return predicted_key_name, feature_matrix, hist, extra_features

def findKeyUsingCQT(filename): 
    return findKey(filename, method = METHOD_CQT)

def findKeyUsingLombScargle(filename): 
    return findKey(filename, method = METHOD_LOMB_SCARGLE)

def searchForBestProfile():
    dataset = pickle.load(open("profile_dataset.npy", "rb"))

    for m in range(100):
        distances = np.zeros(24)
        tp, fp, relatives, parallels, out_by_a_fifth, out_by_a_fourth, n_total = 0, 0, 0, 0, 0, 0, 0

        epsilon = 0.5
        alpha, gamma = np.random.rand(12), np.random.rand(12)
        major = alpha * (CUSTOM_MAJOR_BASE_PROFILE-epsilon) + (1.0 - alpha) * (CUSTOM_MAJOR_BASE_PROFILE+epsilon)
        major_profile_matrix = createProfileMatrix(major)
        minor = gamma * (CUSTOM_MINOR_BASE_PROFILE-epsilon) + (1.0 - gamma) * (CUSTOM_MINOR_BASE_PROFILE+epsilon)
        minor_profile_matrix = createProfileMatrix(minor)

        p = np.random.rand(1)[0]
        print("p : %s" % str(p))
        for i, row in enumerate(dataset):
            hist = np.zeros(24, dtype = np.int)
            (spectral_matrix, target_key) = row
            for j in range(len(spectral_matrix)):
                coefs = spectral_matrix[j]
                coefs = np.reshape(coefs, (Parameters.n_octaves, 12))
                coefs = p * coefs.max(axis = 0) + (1.0 - p) * coefs.sum(axis = 0)
                hist[matchWithProfiles(
                    coefs,
                    major_profile_matrix,
                    minor_profile_matrix)] += 1
            predicted_key = predictKeyFromHistogram(hist)
            distance = getDistance(predicted_key, target_key)
            distances[distance] += 1

            n_total += 1

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
        print(major)
        print(minor)
        print(distances)
        print(tp, parallels, relatives, out_by_a_fifth, out_by_a_fourth, fp)
        print("")

if __name__ == "__main__":
    searchForBestProfile()