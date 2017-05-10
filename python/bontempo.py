# -*- coding: utf-8 -*-
# bontempo.py
# author : Antoine Passemiers

import numpy as np

class BeatDetector:
    def __init__(self):
        self.frame_size = 128
    def detect(self, signal):
        n_slides = len(signal) / self.frame_size
        ste = np.empty(n_slides, dtype = np.double)
        beats = np.empty(n_slides, dtype = np.bool)
        for i in range(n_slides):
            frame = signal[self.frame_size*i:self.frame_size*(i+1)]
            ste[i] = np.sum(frame ** 2)
        ticks = np.where(ste > ste.mean() * 2)[0]
        # bpm = 60.0 * 4410.0 / (np.diff(ticks).mean() * self.frame_size)
        return ticks * self.frame_size + self.frame_size / 2