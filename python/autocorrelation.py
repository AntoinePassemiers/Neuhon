# -*- coding: utf-8 -*-
# autocorrelation.py
# author : Antoine Passemiers

from cognitive import *


def alignArrayWithItself(arr, size, offset):
    return arr[offset:(size + offset)]

def getRho3DMatrix(signal):
    i, n_vectors = 0, 0
    n_coefs = Parameters.window_size
    n_samples = len(signal)
    T = n_samples - Parameters.window_size
    # blackman_win = np.blackman(Parameters.window_size)
    rho = np.empty(
        (T / Parameters.window_size + 1, 
            len(Parameters.note_periods) + 1,
            len(Parameters.note_periods) + 1), 
        dtype = np.double)
    while i < T:
        frame = signal[i:i+Parameters.window_size]
        all_slides = np.empty((len(frame) / 2, len(Parameters.note_periods) + 1))
        for p, period in enumerate(Parameters.note_periods):
            slided_frame = alignArrayWithItself(frame, len(frame) / 2, period)
            all_slides[:, p + 1] = slided_frame

        sigma = np.cov(all_slides.T)
        inv_diag = 1.0 / np.diag(sigma)
        rho[n_vectors, :, :] = inv_diag.T * sigma * inv_diag
        i += Parameters.window_size
        n_vectors += 1
    return rho

def getPseudoSpectra(rho):
    for i in range(len(rho)):
        eigenvalues, eigenvectors = np.linalg.eig(rho[i])
        # eigenvectors[:, i] is the ith vector
        # TODO : compute Pisarenko function

def findKeyUsingAutocorrelation(filename):
    hist = {
        "C" : 0, "C#" : 0, "D" : 0, "Eb" : 0, "E" : 0, "F" : 0, "F#" : 0, "G" : 0,
        "G#" : 0, "A" : 0, "Bb" : 0, "B" : 0, "Cm" : 0, "C#m" : 0, "Dm" : 0, "Ebm" : 0,
        "Em" : 0, "Fm" : 0, "F#m" : 0, "Gm" : 0, "G#m" : 0, "Am" : 0, "Bbm" : 0, "Bm" : 0
    }
    """ Loading wav file """
    stereo_signal = getSignalFromFile(filename)
    """ Averaging the 2 channels (stereo -> mono) """
    signal = stereoToMono(stereo_signal) # Mean of left and right channels
    """ Low-pass filtering """
    signal = lowPassFiltering(signal)
    """ Downsampling """
    signal = downSampling(signal, framerate = 4410.0)
    """ Computing Short-Term Energies """
    ste_sequence, zcr_sequence = getSTEandZCRs(signal)
    """ Compute auto-correlation matrices """
    rho = getRho3DMatrix(signal)
    """ Compute eigenvalues and eigenvectors """

    return "C", None # TODO