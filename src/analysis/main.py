# This is a sample Python script.

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.

from scipy.stats import ttest_ind
from matplotlib import pyplot
import numpy as np


DATA_FOLDER = "../../data/"


def read_observations(file_path):
    with open(file_path, "r") as inp_file:
        observations = []
        for line in inp_file.readlines():
            sample = [float(member) for member in line.strip().split()]
            mean = np.array(sample).mean()
            print(mean)
            observations.append(mean)

        return np.array(observations)


def test_for_identity(observations1, observations2):
    result = wilcoxon(observations1, observations2)
    level = 0
    if result[1] < 0.0005:
        level = 3
    elif result[1] < 0.01:
        level = 2
    elif result[1] < 0.05:
        level = 1

    print(result)
    print("Statistical difference level: {}".format(level))


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    observations1 = read_observations(DATA_FOLDER + "MyMCTSAgent3+LA+TR+SP+MM+HP.txt")
    observations2 = read_observations(DATA_FOLDER + "RobinBaumgartenAgent3.txt")
    pyplot.hist(observations1, color="r")
    pyplot.hist(observations2, color="b")
    pyplot.show()
    print(ttest_ind(observations1, observations2))

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
