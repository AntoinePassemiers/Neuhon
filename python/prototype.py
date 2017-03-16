# -*- coding: utf-8 -*-

import os, operator
import numpy as np
import matplotlib.pyplot as plt
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr

from utils import *

CSV_PATH = "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"
WAV_PATH = "D://KeyFinderDB"


def createProfileMatrix(profile):
	mat = np.empty((12, 12), dtype = np.float)
	for i in range(0, 12):
		mat[i, :] = np.roll(profile, i)
	return mat

KRUMHANSL_MAJOR_BASE_PROFILE = np.array([6.4, 2.2, 3.5, 2.3, 4.4, 4.1, 2.5, 5.2, 2.4, 3.7, 2.3, 2.9])
KRUMHANSL_MINOR_BASE_PROFILE = np.array([6.4, 2.8, 3.6, 5.4, 2.7, 3.6, 2.6, 4.8, 4.0, 2.7, 3.3, 3.2])
SHAATH_MAJOR_BASE_PROFILE    = np.array([6.6, 2.0, 3.5, 2.2, 4.6, 4.0, 2.5, 5.2, 2.4, 3.8, 2.3, 3.4])
SHAATH_MINOR_BASE_PROFILE    = np.array([6.5, 2.8, 3.5, 5.4, 2.7, 3.5, 2.5, 5.1, 4.0, 2.7, 4.3, 3.2])
MAJOR_PROFILE_MATRIX = createProfileMatrix(SHAATH_MAJOR_BASE_PROFILE)
MINOR_PROFILE_MATRIX = createProfileMatrix(SHAATH_MINOR_BASE_PROFILE)

KEY_NAMES = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B"]
KEY_DICT = { keyname : i for i, keyname in enumerate(KEY_NAMES) }

def midiToHertz(d):
	return 440.0 * 2.0 ** ((d - 69.0) / 12.0)

def w_xk(x, lk, rk):
	return 1.0 - np.cos(2 * np.pi * (x - lk) / (rk - lk))

def QFromP(p):
	return p * (np.power(2.0, 1.0 / 12.0) - 1.0)

def winBounds(Q, fk, N, sampling_rate):
	rang = fk * N / float(sampling_rate)
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

def downSampling(signal):
	indexes = np.asarray(np.arange(
		0, len(signal), 
		float(Parameters.sampling_rate) / Parameters.lowpass_filter_cutoff_freq
	), dtype = np.int)
	signal = signal[indexes]
	return signal

def getSpectralWindows(n_samples):
	assert(Parameters.n_octaves == (Parameters.max_midi_note - Parameters.min_midi_note) / 12)
	assert((Parameters.max_midi_note - Parameters.min_midi_note) % 12 == 0)
	fft_freqs = np.fft.fftfreq(Parameters.window_slide) * Parameters.target_sampling_rate
	Q = QFromP(0.8)
	wins = list()
	for fk in midiToHertz(np.arange(Parameters.min_midi_note, Parameters.max_midi_note)):
		lk, rk = winBounds(Q, fk, Parameters.window_slide, Parameters.target_sampling_rate)
		li, ri = findNearestBoundsInSpectrum(fft_freqs, lk, rk)
		win = w_xk(fft_freqs[li:ri+1], lk, rk)
		win /= win.sum()
		wins.append((li, ri, win))
	return wins

def getSTEandZCRs(signal):
	i, n_vectors = 0, 0
	n_samples = len(signal)
	T = n_samples - Parameters.window_slide
	blackman_win = np.blackman(Parameters.window_slide)
	ste_sequence = np.empty(T / Parameters.window_slide + 1, dtype = np.double)
	zcr_sequence = np.empty(T / Parameters.window_slide + 1, dtype = np.double)
	while i < T:
		frame = blackman_win * signal[i:i+Parameters.window_slide]
		ste_sequence[n_vectors] = (frame ** 2).sum()
		zcr_sequence[n_vectors] = len(np.where(np.diff(np.signbit(signal)))[0])
		i += Parameters.window_slide
		n_vectors += 1
	return ste_sequence, zcr_sequence

def getFFTs(signal):
	i, n_vectors = 0, 0
	n_coefs = Parameters.window_slide
	n_samples = len(signal)
	T = n_samples - Parameters.window_slide
	blackman_win = np.blackman(Parameters.window_slide)
	fft_matrix = np.empty((T / Parameters.window_slide + 1, n_coefs), dtype = np.double)
	while i < T:
		frame = blackman_win * signal[i:i+Parameters.window_slide]
		fft_matrix[n_vectors, :] = np.abs(np.fft.fft(frame))
		i += Parameters.window_slide
		n_vectors += 1
	return fft_matrix

def getCQTs(fft_matrix, wins):
	n_vectors = 0
	n_coefs = len(wins)
	cqt_matrix = np.empty((len(fft_matrix), n_coefs), dtype = np.double)
	while n_vectors < len(fft_matrix):
		for k in range(n_coefs):
			li, ri, win = wins[k]
			cqt_matrix[n_vectors, k] = (win * fft_matrix[n_vectors, li:ri+1]).sum()
		n_vectors += 1
	return cqt_matrix

def findKey(filename):
	""" Loading wav file """
	stereo_signal = getSignalFromFile(filename)
	""" Averaging the 2 channels (stereo -> mono) """
	signal = stereoToMono(stereo_signal) # Mean of left and right channels
	""" Low-pass filtering """
	signal = lowPassFiltering(signal)
	""" Downsampling """
	signal = downSampling(signal)
	""" Spectral windows for getting CQT from real spectrum """
	wins = getSpectralWindows(len(signal))

	hist = {
		"C" : 0, "C#" : 0, "D" : 0, "Eb" : 0, "E" : 0, "F" : 0, "F#" : 0, "G" : 0,
		"G#" : 0, "A" : 0, "Bb" : 0, "B" : 0, "Cm" : 0, "C#m" : 0, "Dm" : 0, "Ebm" : 0,
		"Em" : 0, "Fm" : 0, "F#m" : 0, "Gm" : 0, "G#m" : 0, "Am" : 0, "Bbm" : 0, "Bm" : 0
	}

	""" Computing Short-Term Energies """
	ste_sequence, zcr_sequence = getSTEandZCRs(signal)
	""" Computing real Fast Fourier Transforms """
	fft_matrix = getFFTs(signal)
	""" Computing Constant-Q Transforms """
	cqt_matrix = getCQTs(fft_matrix, wins)

	n_samples = len(cqt_matrix)
	chromatic_matrix = np.empty((n_samples, 12), dtype = np.double)
	n_vectors = 0
	while n_vectors < n_samples:
		coefs = cqt_matrix[n_vectors]
		coefs = np.reshape(coefs, (Parameters.n_octaves, 12))
		p = Parameters.chromatic_max_weight
		coefs = p * coefs.max(axis = 0) + (1.0 - p) * coefs.sum(axis = 0)
		chromatic_matrix[n_vectors, :] = coefs[:]
		
		major_scores, minor_scores = np.zeros(12), np.zeros(12)
		for j in range(12):
			major_scores[j] = pearsonr(coefs, MAJOR_PROFILE_MATRIX[j])[0]
			minor_scores[j] = pearsonr(coefs, MINOR_PROFILE_MATRIX[j])[0]

		best_major_key = major_scores.argmax()
		best_minor_key = minor_scores.argmax()
		best_score = max(major_scores[best_major_key], minor_scores[best_minor_key])
		if major_scores[best_major_key] > minor_scores[best_minor_key]:
			kk = KEY_NAMES[(Parameters.min_midi_note - 1 + best_major_key) % 12]
		else:
			kk = KEY_NAMES[(Parameters.min_midi_note - 1 + best_minor_key) % 12] + "m"
		if best_score >= 0.0:
			hist[kk] += np.log(ste_sequence[n_vectors])
		n_vectors += 1
	predicted_key_name = max(hist.iteritems(), key = operator.itemgetter(1))[0]
	return predicted_key_name, chromatic_matrix

def getDistance(predicted_key, target_key):
	predicted_key_index = KEY_DICT[predicted_key.replace("m", "")]
	target_key_index = KEY_DICT[target_key.replace("m", "")]
	return (predicted_key_index - target_key_index + 12) % 12

def isParallel(predicted_key, target_key):
	return getDistance(predicted_key, target_key) == 0 and predicted_key != target_key

def isOutByAFifth(predicted_key, target_key):
	rel = ("m" in predicted_key) == ("m" in target_key)
	distance = getDistance(predicted_key, target_key)
	return rel and (distance == 5 or distance == 7)

def isRelative(predicted_key, target_key):
	distance = getDistance(predicted_key, target_key)
	if "m" in predicted_key:
		return (not "m" in target_key) and distance == 9
	else:
		return "m" in target_key and distance == 3

def useCognitiveModel():
	csv_file = open(CSV_PATH, "r")
	csv_file.readline()
	tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0
	distances = np.zeros(12)

	for i in range(230):
		row = csv_file.readline().replace('\n', '').split(';')
		artist, title, target_key, filename = row[0], row[1], row[2], row[3]

		predicted_key, _ = findKey(filename)
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

		showWavFileResults(i, artist, title, target_key, predicted_key)
	showFinalResults(tp, out_by_a_fifth, parallels, relatives, fp, n_total)
	print(distances)
	print("Finished")
	csv_file.close()

if __name__ == "__main__":
	useCognitiveModel()