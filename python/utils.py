# -*- coding: utf-8 -*-
# utils.py
# author : Antoine Passemiers

import sys

class Parameters:
    sampling_rate = 44100.0
    target_sampling_rate = 4410.0
    n_channels = 2

    lowpass_filter_order = 5
    lowpass_filter_cutoff_freq = 4410.0

    window_slide = 4096 * 4

    min_midi_note = 9
    max_midi_note = 81
    n_octaves = 6

    chromatic_max_weight = 0.8

def todo(func):
    def func_wrapper(*args):
        raise NotImplementedError("%s is not implemented yet" % func.__name__)
    func_wrapper.__name__ = func.__name__
    return func_wrapper

def showWavFileResults(index, artist, title, target_key, predicted_key):
    print("File number %4d" % (index + 1))
    print("----------------")
    print("Artist        : %s" % artist)
    print("Title         : %s" % title)
    print("Target key    : %s" % target_key)
    print("Predicted key : %s\n" % predicted_key)

def showFinalResults(tp, out_by_a_fifth, parallels, relatives, fp, n_total):
    print("Perfect matches : %s" % str(tp))
    print("Out by a fifth : %s" % str(out_by_a_fifth))
    print("Parallel keys : %s" % str(parallels))
    print("Relative keys : %s" % str(relatives))
    print("Wrong keys : %s" % str(fp))
    print("Total : %s" % str(n_total))