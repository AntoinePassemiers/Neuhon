# -*- coding: utf-8 -*-
# main.py
# author : Antoine Passemiers

import sys, pickle

from autocorrelation import *
from cognitive import *
from spectral import *

from sklearn.tree import DecisionTreeClassifier

__all_detection_methods__ = [
    "findKeyUsingCQT",
    "findKeyUsingLombScargle",
    "findKeyUsingAutocorrelation"]

def createTrainingSet():
    dataset_X = list()
    dataset_y = list()
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()
    for i in range(230): # 230
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]
        try:
            _, _, vec, extra = findKeyUsingLombScargle(filename)
            dataset_X.append(vec / float(np.sum(vec)))
            dataset_y.append(KEY_DICT[target_key])
        except IOError:
            pass
    np.save("dataset_X", np.array(dataset_X))
    np.save("dataset_y", np.array(dataset_y))

def fitModel():
    dataset_X = np.load("dataset_X.npy")
    dataset_y = np.load("dataset_y.npy")

    n = 150
    train_X, validation_X = dataset_X[:n], dataset_X[n:]
    train_y, validation_y = dataset_y[:n], dataset_y[n:]

    tree = DecisionTreeClassifier()
    tree.fit(train_X, train_y)
    predictions = tree.predict(validation_X)
    print(np.sum(predictions == validation_y), len(predictions))

def main(prediction_func):
    csv_file = open(CSV_PATH, "r")
    csv_file.readline()
    tp, fp, relatives, parallels, out_by_a_fifth, n_total = 0, 0, 0, 0, 0, 0
    distances = np.zeros(24)
    chromatic_dataset = list()
    for i in range(1): # 230
        row = csv_file.readline().replace('\n', '').split(';')
        artist, title, target_key, filename = row[0], row[1], row[2], row[3]

        try:
            predicted_key, chromatic_matrix, _, _ = prediction_func(filename)
            chromatic_dataset.append((chromatic_matrix, target_key))
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
    pickle.dump(chromatic_dataset, open("profile_dataset.npy", "wb"))
    print(distances)

if __name__ == "__main__":
    main(findKeyUsingLombScargle)
    # createTrainingSet()
    # fitModel()
    print("Finished")