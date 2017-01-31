# -*- coding: utf-8 -*-

import numpy as np
import os, operator
from scipy.io.wavfile import read as scipy_read
from scipy.signal import butter, lfilter, freqz
from scipy.stats import pearsonr
import matplotlib.pyplot as plt

CSV_PATH = "D://KeyFinderDB/DOC/KeyFinderV2Dataset.csv"
WAV_PATH = "D://KeyFinderDB"

def butter_lowpass_filter(data, cutoff, fs, order = 5):
    nyq = 0.5 * fs
    normal_cutoff = cutoff / nyq
    b, a = butter(order, normal_cutoff, btype = 'low', analog = False)
    return lfilter(b, a, data)

def createProfileMatrix(profile):
	mat = np.empty((12, 12), dtype = np.float)
	for i in range(0, 12):
		mat[i, :] = np.roll(profile, i) # LEFT ROTATION !
	return mat

KRUMHANSL_MAJOR_BASE_PROFILE = np.array([6.4, 2.2, 3.5, 2.3, 4.4, 4.1, 2.5, 5.2, 2.4, 3.7, 2.3, 2.9])
KRUMHANSL_MINOR_BASE_PROFILE = np.array([6.4, 2.8, 3.6, 5.4, 2.7, 3.6, 2.6, 4.8, 4.0, 2.7, 3.3, 3.2])
SHAATH_MAJOR_BASE_PROFILE = np.array([6.6, 2.0, 3.5, 2.2, 4.6, 4.0, 2.5, 5.2, 2.4, 3.8, 2.3, 3.4])
SHAATH_MINOR_BASE_PROFILE = np.array([6.5, 2.8, 3.5, 5.4, 2.7, 3.5, 2.5, 5.1, 4.0, 2.7, 4.3, 3.2])
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

def findKey(filename):
	filepath = os.path.join(WAV_PATH, filename)
	framerate, signal = scipy_read(filepath)
	assert(framerate == 44100)
	assert(signal.shape[1] == 2)
	signal = signal.mean(axis = 1) # Mean of left and right channels
	signal = butter_lowpass_filter(signal, 2205.0, 44100.0, order = 5) # Low-pass filtering # TODO : Fisher's filter
	signal = signal[::10] # Downsampling -> 4410 Hz
	sampling_rate = 4410.0
	N = 4096 * 4
	min_midi_note, max_midi_note = 9, 81
	n_octaves = 6
	assert(n_octaves == (max_midi_note - min_midi_note) / 12)
	assert((max_midi_note - min_midi_note) % 12 == 0)
	fft_freqs = np.fft.fftfreq(N) * sampling_rate
	T = len(signal) - N
	Q = QFromP(0.8)
	wins = list()
	for fk in midiToHertz(np.arange(min_midi_note, max_midi_note)):
		lk, rk = winBounds(Q, fk, N, sampling_rate)
		li, ri = findNearestBoundsInSpectrum(fft_freqs, lk, rk)
		win = w_xk(fft_freqs[li:ri+1], lk, rk)
		win /= win.sum()
		wins.append((li, ri, win))

	i = 0
	hist = {
		"C" : 0, "C#" : 0, "D" : 0, "Eb" : 0, "E" : 0, "F" : 0, "F#" : 0, "G" : 0,
		"G#" : 0, "A" : 0, "Bb" : 0, "B" : 0, "Cm" : 0, "C#m" : 0, "Dm" : 0, "Ebm" : 0,
		"Em" : 0, "Fm" : 0, "F#m" : 0, "Gm" : 0, "G#m" : 0, "Am" : 0, "Bbm" : 0, "Bm" : 0
	}
	blackman_win = np.blackman(N)
	while i < T:
		frame = blackman_win * signal[i:i+N]
		ste = (frame ** 2).sum()
		f = np.abs(np.fft.fft(frame))
		coefs = list()
		for k in range(len(wins)):
			li, ri, win = wins[k]
			coefs.append((win * f[li:ri+1]).sum())
		coefs = np.array(coefs)
		coefs = np.reshape(coefs, (n_octaves, 12))
		coefs = 0.8 * coefs.max(axis = 0) + 0.2 * coefs.sum(axis = 0)
		
		major_scores, minor_scores = np.zeros(12), np.zeros(12)
		for j in range(12):
			major_scores[j] = pearsonr(coefs, MAJOR_PROFILE_MATRIX[j])[0]
			minor_scores[j] = pearsonr(coefs, MINOR_PROFILE_MATRIX[j])[0]

		best_major_key = major_scores.argmax()
		best_minor_key = minor_scores.argmax()
		best_score = max(major_scores[best_major_key], minor_scores[best_minor_key])
		if major_scores[best_major_key] > minor_scores[best_minor_key]:
			kk = KEY_NAMES[(min_midi_note - 1 + best_major_key) % 12]
		else:
			kk = KEY_NAMES[(min_midi_note - 1 + best_minor_key) % 12] + "m"
		if best_score >= 0.0:
			hist[kk] += np.log(ste) # TODO
		i += N
		# i += (N / 4)
	predicted_key_name = max(hist.iteritems(), key = operator.itemgetter(1))[0]
	return predicted_key_name

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

if __name__ == "__main__":
	csv_file = open(CSV_PATH, "r")
	csv_file.readline()
	tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0
	distances = np.zeros(12)
	for i in range(230):
		row = csv_file.readline().replace('\n', '').split(';')
		artist = row[0]
		title = row[1]
		target_key = row[2]
		filename = row[3]

		try:
			predicted_key = findKey(filename)
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
		except:
			predicted_key = "ERROR !"

		print("File number %4d" % (i + 1))
		print("----------------")
		print("Artist        : %s" % artist)
		print("Title         : %s" % title)
		print("Target key    : %s" % target_key)
		print("Predicted key : %s\n" % predicted_key)
	print("Perfect matches : %s" % str(tp))
	print("Out by a fifth : %s" % str(out_by_a_fifth))
	print("Parallel keys : %s" % str(parallels))
	print("Relative keys : %s" % str(relatives))
	print("Wrong keys : %s" % str(fp))
	print("Total : %s" % str(n_total))
	print(distances)
	print("Finished")