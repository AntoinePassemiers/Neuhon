# -*- coding: utf-8 -*-
# cognitive.py
# author : Antoine Passemiers

import os, operator, pickle
import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr

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
    [ 8.85003345,  2.55626553,  4.15097735,  7.0429511,   4.69613877,  3.37875891,
     -0.09147103,  6.04799802,  3.4338048,  -0.37593573,  3.97494554,  5.64818483])
CUSTOM_MINOR_BASE_PROFILE    = np.array(
    [ 6.4870956,   3.23523279,  3.07355565,  3.45581634,  4.85043354,  4.96966877,
      2.75298759,  6.39271897,  0.49290184,  5.12401634,  1.67300141,  4.63196307])

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

def getFFTs(signal):
    i, n_vectors = 0, 0
    n_coefs = Parameters.window_size
    n_samples = len(signal)
    T = n_samples - Parameters.window_size
    blackman_win = np.blackman(Parameters.window_size)
    fft_matrix = np.empty((T / Parameters.window_size + 1, n_coefs), dtype = np.double)
    while i < T:
        frame = blackman_win * signal[i:i+Parameters.window_size]
        fft_matrix[n_vectors, :] = np.abs(np.fft.fft(frame))
        i += Parameters.window_size
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
    ste_sequence, zcr_sequence = getSTEandZCRs(signal)
    extra_features = ExtraFeatures()
    extra_features.STE = ste_sequence
    extra_features.ZCR = zcr_sequence

    if method == METHOD_CQT:
        """ Computing real Fast Fourier Transforms """
        fft_matrix = getFFTs(signal)
        """ Computing Constant-Q Transforms """
        feature_matrix = getCQTs(fft_matrix, wins)
    elif method == METHOD_LOMB_SCARGLE:
        """ Computing Lomb-Scargle periodograms """
        feature_matrix = getPeriodograms(signal)
        # print(list(feature_matrix[30]))

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
        hist[kk] += 1
        n_vectors += 1

    print(hist.reshape(2, 12))
    predicted_key_name = predictKeyFromHistogram(hist)
    return predicted_key_name, chromatic_matrix, hist, extra_features

def findKeyUsingCQT(filename): 
    return findKey(filename, method = METHOD_CQT)

def findKeyUsingLombScargle(filename): 
    return findKey(filename, method = METHOD_LOMB_SCARGLE)

def searchForBestProfile():
    dataset = pickle.load(open("profile_dataset.npy", "rb"))

    for m in range(60):
        distances = np.zeros(24)
        tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0

        epsilon = 0.15
        alpha, gamma = np.random.rand(12), np.random.rand(12)
        major = alpha * (CUSTOM_MAJOR_BASE_PROFILE-epsilon) + (1.0 - alpha) * (CUSTOM_MAJOR_BASE_PROFILE+epsilon)
        major_profile_matrix = createProfileMatrix(major)
        minor = gamma * (CUSTOM_MINOR_BASE_PROFILE-epsilon) + (1.0 - gamma) * (CUSTOM_MINOR_BASE_PROFILE+epsilon)
        minor_profile_matrix = createProfileMatrix(minor)

        for i, row in enumerate(dataset):
            hist = np.zeros(24, dtype = np.int)
            (chromatic_matrix, target_key) = row
            for j in range(len(chromatic_matrix)):
                hist[matchWithProfiles(
                    chromatic_matrix[j],
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
            else:
                fp += 1
        print(major)
        print(minor)
        print(distances)
        print(tp, parallels, relatives, out_by_a_fifth, fp)
        print("")

if __name__ == "__main__":
    searchForBestProfile()
    print("Finished")