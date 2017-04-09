# -*- coding: utf-8 -*-
# main.py
# author : Antoine Passemiers

"""
                                       PM   OBF  PK   RK   WK   TOTAL
Best score using the CQT method :      58   28   18   8    77   189
"""

import sys

from autocorrelation import *
from cognitive import *
from spectral import *

from sklearn.tree import DecisionTreeClassifier

__all_detection_methods__ = [
    "findKeyUsingCQT",
    "findKeyUsingLombScargle",
    "findKeyUsingAutocorrelation"]

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

def createTrainingSet():
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()
    dataset_X = list()
    dataset_y = list()
    for i in range(230): # 230
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]
        try:
            _, _, vec = findKeyUsingLombScargle(filename)
            dataset_X.append(vec)
            dataset_y.append(KEY_DICT[target_key])
        except IOError:
            pass
    np.save("dataset_X", np.array(dataset_X))
    np.save("dataset_y", np.array(dataset_y))

def main(prediction_func):
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()
    tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0
    distances = np.zeros(24)

    for i in range(4): # 230
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]

        try:
            predicted_key, _, _, _ = prediction_func(filename)
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
        except IOError:
            pass
    showFinalResults(tp, out_by_a_fifth, parallels, relatives, fp, n_total)
    csv_file.close()
    print(distances)

if __name__ == "__main__":
    # sys.stdout = open('py_output.txt', 'w')
    main(findKeyUsingLombScargle)
    # MLForGlobalPredictions()
    print("Finished")