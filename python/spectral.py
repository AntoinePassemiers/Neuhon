# -*- coding: utf-8 -*-
# spectral.py - Least squares spectral analysis
# author : Antoine Passemiers

import numpy as np
import random

from utils import Parameters


class VanicekRegressor:
	""" Least squares regressor for fitting samples with their corresponding
	spectrum by infering the spectral coefficients. This implementation is based
	on the Vaníček method. The spectral coefficients are given by :
	x = inv(A.T * A) * A.T * psi,
	where psi is the input samples vector,
	A is a matrix containing known sinusoidal samples,
	and x is the pseudo-spectrum.

	Parameters
	----------
	window_size : int
	    Number of input samples per window
	sampling_rate : float
	    Sampling rate of the input samples

	Attributes
	----------
	A : np.ndarray[ndim = 2]
	    Matrix where each row is a sinusoide of given frequency
	LRPM : np.ndarray[ndim = 2]
	    LRPM := inv(A.T * A) * A.T

	"""
	def __init__(self, window_size, sampling_rate):
		""" Precomputes what can be precomputed for the linear regression """
		self.window_size = window_size
		self.sampling_rate = sampling_rate
		self.A, self.phases = self.matrixOfSinusoidals(window_size, sampling_rate)
		self.LRPM = self.linearRegressionPreprocessing(self.A).T

	def matrixOfSinusoidals(self, window_size, sampling_rate):
		""" Precomputes the matrix A with random phase changes.
		Indeed, if every sinusoide is provided with no phase change,
		the resulting matrix A tends to be singular. """
		time = np.arange(window_size)
		A = np.empty((len(Parameters.note_frequencies), window_size), dtype = np.double)
		phases = np.empty(len(Parameters.note_frequencies), dtype = np.double)
		for i, freq in enumerate(Parameters.note_frequencies):
			phases[i] = 2.0 * np.pi * np.random.rand(1)[0]
			sine_wave = np.sin(2.0 * np.pi * time * (freq / sampling_rate) + phases[i])
			A[i, :] = sine_wave[:]
		return A, phases

	def linearRegressionPreprocessing(self, A):
		""" Precomputes the matrix LRPM """
		return np.dot(np.linalg.inv(np.dot(A.T, A)), A.T)

	def fit(self, psi):
		""" Computes x = LRPM * psi """
		return np.dot(self.LRPM, psi)


class LombScargleRegressor:
	""" Least squares regressor based on the Lomb-Scargle method, which
	takes into account the phase changes. 

	Parameters
	----------
	window_size : int
	    Number of input samples per window
	sampling_rate : float
	    Sampling rate of the input samples

	Attributes
	----------
	taus : np.ndarray[ndim = 1]
	    Phase changes of each of the given frequencies
	"""
	def __init__(self, window_size, sampling_rate):
		""" Precomputes what can be precomputed for Lomb-Scargle method """
		self.window_size = window_size
		self.sampling_rate = sampling_rate
		self.taus = self.timeDelays(window_size, sampling_rate)

		""" Preprocessed variables """
		time = np.arange(window_size)
		self.cos_waves, self.sin_waves = list(), list()
		self.dens_a, self.dens_b = list(), list()
		for i, freq in enumerate(Parameters.note_frequencies):
			tmp = 2.0 * np.pi * (time - self.taus[i]) * (freq / sampling_rate)
			self.cos_waves.append(np.cos(tmp))
			self.sin_waves.append(np.sin(tmp))
			self.dens_a.append(np.sum(np.cos(tmp) ** 2))
			self.dens_b.append(np.sum(np.sin(tmp) ** 2))

	def timeDelays(self, window_size, sampling_rate):
		""" Computes the phases changes of the given frequencies """
		time = np.arange(window_size)
		taus = np.empty(len(Parameters.note_frequencies), dtype = np.double)
		for i, freq in enumerate(Parameters.note_frequencies):
			numerator = np.sin(2.0 * np.pi * time * (freq / sampling_rate)).sum()
			denominator = np.cos(2.0 * np.pi * time * (freq / sampling_rate)).sum()
			taus[i] = np.arctan(numerator / denominator) / (2.0 * np.pi * freq / sampling_rate)
		return taus

	def fit(self, psi):
		""" Computes the periodogram from input samples """
		psi = psi[:self.window_size]
		time = np.arange(self.window_size)
		Px = np.empty(len(Parameters.note_frequencies), dtype = np.double)
		for i, freq in enumerate(Parameters.note_frequencies):
			num_a = np.sum(psi * self.cos_waves[i]) ** 2
			num_b = np.sum(psi * self.sin_waves[i]) ** 2
			Px[i] = 0.5 * (num_a / self.dens_a[i] + num_b / self.dens_b[i])
		return Px

def getPeriodograms(signal):
	window_size = Parameters.window_size
	sampling_rate = Parameters.target_sampling_rate

	regressor = LombScargleRegressor(window_size, sampling_rate)

	i, n_vectors = 0, 0
	n_coefs = len(Parameters.note_frequencies)
	n_samples = len(signal)
 	T = n_samples - Parameters.window_size
 	# blackman_win = np.blackman(Parameters.window_size)
	periodograms = np.empty(
		((T + Parameters.window_size) / Parameters.slide + 1, n_coefs), 
		dtype = np.double)
	while i < T:
		frame = signal[i:i+Parameters.window_size]
		periodograms[n_vectors, :] = regressor.fit(frame)
		i += Parameters.slide
		n_vectors += 1
	return periodograms


if __name__ == "__main__":
	sampling_rate = 4410.0
	regressor = LombScargleRegressor(16384, sampling_rate)
	# print([(freq, regressor.taus[i]) for i, freq in enumerate(Parameters.note_frequencies)])

	window_size = Parameters.window_size
	freq = 440.0
	s = np.cos(2.0 * np.pi * np.arange(window_size) * freq / sampling_rate)
	# print(list(regressor.fit(s)))