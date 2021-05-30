# This is a sample Python script.

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.

from scipy.stats import ttest_ind, normaltest
from statsmodels.stats.power import TTestIndPower
from matplotlib import pyplot
import numpy as np
import os

DATA_FOLDER = "../../data/EnhancementsResIV"
RESAMPLE_SIZE = 10000


def read_observations(file_path):
    with open(file_path, "r") as inp_file:
        observations = []
        for line in inp_file.readlines():
            sample = [float(member) for member in line.strip().split()]
            mean = np.array(sample).mean()
            observations.append(mean)

        return np.array(observations)


def read_sample(file_path) -> np.array:
    with open(file_path, "r") as inp_file:
        observations = []
        for line in inp_file.readlines():
            sample = [float(member) for member in line.strip().split()]
            observations.append(sample)

        return np.array(observations)[0]


def test_for_identity(observations1, observations2):
    result = ttest_ind(observations1, observations2)
    p_value = result[1]

    return significance_level(p_value)


def significance_level(p_value):
    if p_value < 0.0005:
        level = 3
    elif p_value < 0.01:
        level = 2
    elif p_value < 0.05:
        level = 1
    else:
        level = 0
    if level > 0:
        return f"p-value: {p_value}; Samples are different with significance level: {level}"
    else:
        return f"p-value: {p_value}; Samples have no statistically significant difference"


def calc_cohen_d(group1, group2):
    """Calculate and Define Cohen's d"""
    # group1: Series or NumPy array
    # group2: Series or NumPy array
    # returns a floating point number
    diff = group1.mean() - group2.mean()
    n1, n2 = len(group1), len(group2)
    var1 = group1.var()
    var2 = group2.var()
    # Calculate the pooled threshold as shown earlier
    pooled_var = (n1 * var1 + n2 * var2) / (n1 + n2)

    # Calculate Cohen's d statistic
    d = diff / np.sqrt(pooled_var)

    return d


def calc_recommended_sample_size(sample1, sample2):
    # Define Variables
    effect = calc_cohen_d(sample1, sample2)
    alpha = 0.0005
    power = 0.99
    # sample 2 / sample 1
    ratio = len(sample1) / len(sample2)
    # Perform power analysis
    analysis = TTestIndPower()
    result = analysis.solve_power(effect, power=power, nobs1=None, ratio=ratio, alpha=alpha)
    return f"The minimum sample size: {result}"


def bootstrap_means_distribution(sample):
    bootstrap_samples = []
    for i in range(RESAMPLE_SIZE):
        bootstrap_samples.append(np.random.choice(sample, size=len(sample), replace=True).mean())

    return np.array(bootstrap_samples)


def permutation_test(sample1, sample2):
    original_statistic = abs(sample1.mean() - sample2.mean())
    combined_samples = sample1 + sample2
    N = len(combined_samples)

    number_of_times_permutation_statistic_is_greater = 0

    for i in range(RESAMPLE_SIZE):
        permutation = np.random.choice(combined_samples, size=len(combined_samples), replace=False)
        permutation_statistic = abs(permutation[:N // 2].mean() - permutation[N // 2:].mean())
        if permutation_statistic > original_statistic:
            number_of_times_permutation_statistic_is_greater += 1

    p_value = number_of_times_permutation_statistic_is_greater / RESAMPLE_SIZE
    return significance_level(p_value)


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    for filename in os.listdir(DATA_FOLDER):
        sample = read_sample(DATA_FOLDER + "/" + filename)
        print(f"{filename}: {sample.mean()}")


    # sample1 = read_sample(DATA_FOLDER + "MyMCTSAgent1.txt")
    # sample2 = read_sample(DATA_FOLDER + "RobinBaumgartenAgent1.txt")

    # bootstrap_sample1 = bootstrap_means_distribution(sample1)
    # bootstrap_sample2 = bootstrap_means_distribution(sample2)
    #
    # pyplot.hist(bootstrap_sample1, color="r")
    # pyplot.hist(bootstrap_sample2, color="b")
    # pyplot.show()
    #
    # print(normaltest(bootstrap_sample1))
    # print(normaltest(bootstrap_sample2))
    #
    # print(abs(calc_cohen_d(bootstrap_sample1, bootstrap_sample2)))
    #
    # print(test_for_identity(bootstrap_sample1, bootstrap_sample2))
    #
    # print(permutation_test(sample1, sample2))

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
