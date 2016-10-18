# -*- coding: utf-8 -*-

import numpy as np
import matplotlib.pyplot as plt
import wave, os, struct

DATA_PATH = "C://H Projects/Components"

@np.vectorize
def hertzToMidi(freq):
    return np.round(69.0 + 12.0 * np.log2(freq / 440.0))

@np.vectorize
def midiToHertz(note):
    return 440.0 * 2.0 ** ((note - 69.0) / 12.0)

class Parameters:
    sampling_rate = 44100.0
    win_length = 2048
    minimum_freq = 41.203 # frequency of E1
    maximum_freq = 1046.5 # frequency of C6 

def load16bitsWav(wave_file):
    (nchannels, sampwidth, framerate, nframes, comptype, compname) = wave_file.getparams ()
    frames = wave_file.readframes(nframes * nchannels)
    out = np.array(struct.unpack_from("%dh" % nframes * nchannels, frames))
    return out

def load24bitsWav(wave_file):
    length = wave_file.getnframes()
    signal = np.zeros(length, dtype = np.int32)
    for i in range(0, length):
        waveData = wave_file.readframes(1)
        data = struct.unpack("i", " " + waveData)
        signal[i] = int(data[0])
    return signal

def loadWav(filename):
    wave_file = wave.openfp(filename, 'r')
    n_bytes = wave_file.getsampwidth()
    if n_bytes == 3:
        return np.fromfile(open(filename), np.int32)[24:], n_bytes
    else:
        return load16bitsWav(wave_file), n_bytes
    
def AMDF(data):
    best_index = 0
    best_amdf = 99999999
    N = data.size
    for i in range(Parameters.notes_periods.size):
        amdf = 0
        tau = Parameters.notes_periods[i]
        w = N - tau
        for j in range(w):
            amdf += abs(data[j] - data[j + tau])
        amdf /= w
        if amdf < best_amdf:
            best_amdf = amdf
            best_index = i
    return Parameters.notes_periods[best_index]

def QCT(signal, bins_per_octave = 24, sampling_freq = Parameters, 
        f0 = Parameters.minimum_freq):
    b = bins_per_octave
    Q = 1.0 / (2.0 ** (1.0 / float(b)) - 1)
    N = np.empty(b, dtype = np.int16)
    f = np.empty(b, dtype = np.float32)
    wins = []
    for k in range(b):
        f[k] = f0 * 2.0 ** (float(k) / float(b))
        N[k] = Q * sampling_freq / f[k]
        wins.append(np.hanning(N[k]))
        
    

if __name__ == "__main__":
    signal = loadWav(os.path.join(DATA_PATH, "Uppermost - Born Limitless.wav"))[0]
    win_length = Parameters.win_length
    window = np.hanning(win_length)
    semi_win = win_length / 2
    pitch = []
    for i in range(int(np.floor(len(signal) / int(win_length))) * 2 - 1):
        fft = np.fft.fft(window * signal[(semi_win*i):(semi_win*i+win_length)])
        fft_freq = np.fft.fftfreq(win_length) * Parameters.sampling_rate
        fft = np.abs(fft[:win_length/2])
        fft_freq = fft_freq[:win_length/2]
        a = np.argmax(fft)
        p = hertzToMidi(fft_freq[a])
        if p == -np.inf:
            if len(pitch) == 0:
                p = 0
            else:
                p = pitch[-1]
        print(p)
        pitch.append(fft_freq[a])
    
    plt.plot(pitch[:200])
    # plt.axvline(1500, color = 'r')
    plt.show()
    
    
    
    
    
    
    
    
    