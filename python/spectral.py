# -*- coding: utf-8 -*-
# spectral.py - Least squares spectral analysis
# author : Antoine Passemiers

import numpy as np

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
		time = np.arange(self.window_size)
		Px = np.empty(len(Parameters.note_frequencies), dtype = np.double)
		for i, freq in enumerate(Parameters.note_frequencies):
			num_a = np.sum(psi * np.cos(2.0 * np.pi * (time - self.taus[i]) * (freq / sampling_rate))) ** 2
			num_b = np.sum(psi * np.sin(2.0 * np.pi * (time - self.taus[i]) * (freq / sampling_rate))) ** 2
			den_a = np.sum(np.cos(2.0 * np.pi * (time - self.taus[i]) * (freq / sampling_rate)) ** 2)
			den_b = np.sum(np.sin(2.0 * np.pi * (time - self.taus[i]) * (freq / sampling_rate)) ** 2)
			Px[i] = 0.5 * (num_a / den_a + num_b / den_b)
		return Px

regressor = LombScargleRegressor(16384, 4410.0)
print([(freq, regressor.taus[i]) for i, freq in enumerate(Parameters.note_frequencies)])