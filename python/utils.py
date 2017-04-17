# -*- coding: utf-8 -*-
# utils.py
# author : Antoine Passemiers

import sys
import numpy as np

sys.stdout = open('py_output.txt', 'w')

KEY_NAMES = ["C", "C#", "D", "Eb", "E", "F", "F#", "G", "G#", "A", "Bb", "B",
    "Cm", "C#m", "Dm", "Ebm", "Em", "Fm", "F#m", "Gm", "G#m", "Am", "Bbm", "Bm"]
KEY_DICT = { keyname : i for i, keyname in enumerate(KEY_NAMES) }

def midiToHertz(note):
    """ Converts from a midi value to a frequency """
    return 440.0 * 2.0 ** ((note - 69.0) / 12.0)

def hertzToMidi(frequency):
    """ Converts from a frequency to a midi note """
    return 69 + 12 * np.log(frequency / 440, 2)

class Parameters:
    """ General parameters """
    sampling_rate = 44100.0
    target_sampling_rate = 4410.0
    n_channels = 2

    """ Filtering parameters """
    lowpass_filter_order = 5
    lowpass_filter_cutoff_freq = 5500.0

    """ Parameters of the sliding window """
    window_size = 4096 * 4
    slide = 4096 * 4

    """ Hyper-parameters (for optimization or validation purposes) """
    min_midi_note = 8  # 15
    max_midi_note = 80 # 75
    n_octaves = 6      # 5
    chromatic_max_weight = 0.8
    note_frequencies = midiToHertz(np.arange(min_midi_note, max_midi_note) - 1)
    note_periods = np.rint(target_sampling_rate / note_frequencies).astype(int)

def todo(func):
    def func_wrapper(*args):
        raise NotImplementedError("%s is not implemented yet" % func.__name__)
    func_wrapper.__name__ = func.__name__
    return func_wrapper

def showWavFileResults(index, artist, title, target_key, predicted_key):
    """ Displays the results for a given wav file, given its metadata """
    print("File number %4d" % (index + 1))
    print("----------------")
    print("Artist        : %s" % artist)
    print("Title         : %s" % title)
    print("Target key    : %s" % target_key)
    print("Predicted key : %s\n" % predicted_key)

def showFinalResults(tp, out_by_a_fifth, parallels, relatives, fp, n_total):
    """ Displays the results for the whole dataset, given the score metrics """
    print("Perfect matches : %s" % str(tp))
    print("Out by a fifth : %s" % str(out_by_a_fifth))
    print("Parallel keys : %s" % str(parallels))
    print("Relative keys : %s" % str(relatives))
    print("Wrong keys : %s" % str(fp))
    print("Total : %s" % str(n_total))

def isDifferentToneType(predicted_key, target_key):
    if "m" in predicted_key and not "m" in target_key:
        return True
    elif "m" in target_key and not "m" in predicted_key:
        return True
    else:
        return False

def getDistance(predicted_key, target_key, with_offset = True):
    predicted_key_index = KEY_DICT[predicted_key.replace("m", "")]
    target_key_index = KEY_DICT[target_key.replace("m", "")]
    distance = (predicted_key_index - target_key_index + 12) % 12
    if isDifferentToneType(predicted_key, target_key) and with_offset:
        distance += 12
    return distance

def isParallel(predicted_key, target_key):
    distance = getDistance(predicted_key, target_key, with_offset = False)
    return (distance == 0) and predicted_key != target_key

def isOutByAFifth(predicted_key, target_key):
    rel = ("m" in predicted_key) == ("m" in target_key)
    distance = getDistance(predicted_key, target_key, with_offset = False)
    return rel and (distance == 5 or distance == 7)

def isRelative(predicted_key, target_key):
    distance = getDistance(predicted_key, target_key, with_offset = False)
    if "m" in predicted_key:
        return (not "m" in target_key) and distance == 9
    else:
        return "m" in target_key and distance == 3