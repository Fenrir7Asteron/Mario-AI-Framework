import pandas as pd
import matplotlib.pyplot as plt
from matplotlib.ticker import PercentFormatter
import numpy as np

if __name__ == '__main__':
    data = np.array([0.014256933, -0.118124478, -0.012218555, 0.055873608, -0.036724062, -0.045014475, -0.035428129, 0.002211202,
                     -0.046942258, 0.020077363, -0.029503747, -0.014693934, 0.009920774, 0.044568440, -0.042651834])

    data_abs = np.abs(data)

    # zipped_data = zip(data_abs, data)
    # data_abs, data = zip(*sorted(zipped_data, reverse=True))

    bar_colors = ["C0" if x > 0 else "C3" for x in data]
    print(bar_colors)

    print(np.sum(data_abs))

    labels = ['HP', 'WU', 'TR', 'MIX', 'NG', 'MA', 'LA', 'AG', 'SP',
              'HP+WU', 'HP+TR', 'WU+TR', 'HP+MIX', 'WU+MIX', 'TR+MIX']

    df = pd.DataFrame({'effects': data_abs, 'colors': bar_colors})
    df.index = labels
    df = df.sort_values(by='effects', axis=0, ascending=False)
    df["cumpercentage"] = df["effects"].cumsum() / df["effects"].sum() * 100

    fig, ax = plt.subplots()
    ax.bar(df.index, df["effects"], color=df["colors"])
    ax.set_ylabel('Absolute Effect')
    ax2 = ax.twinx()

    ax2.plot(df.index, df["cumpercentage"], color="C1", marker="D", ms=7)
    ax2.yaxis.set_major_formatter(PercentFormatter())
    ax2.axhline(y=95, color="C2", linestyle='dashed')
    ax2.set_ylabel('Cumulative Percentage')
    ax2.yaxis.set_label_position("right")


    ax.tick_params(axis="y", colors="C0")
    ax2.tick_params(axis="y", colors="C1")
    # ax3.tick_params(axis="y", colors="C2")

    plt.setp(ax.get_xticklabels(), rotation=30, horizontalalignment='right')
    # plt.xlabel('MCTS Enhancements')
    # plt.ylabel('Absolute Effects')
    plt.gcf().subplots_adjust(right=0.85)

    plt.show()

    # model = Pipeline([('poly', PolynomialFeatures(degree=2, interaction_only=True)),
    #                   ('linear', LinearRegression(fit_intercept=False))])
    # # fit to an order-3 polynomial data
    # x = np.array([
    #     [0, 0],
    #     [-1, 0],
    #     [1, 0],
    #     [-1, 1],
    #     [1, 1],
    #
    #     [1.63, 1],
    #     [1.315, 2],
    #     [1, 3],
    #     [1.63, 3]
    # ])
    # print(x.shape)
    # y = np.array([407, 193, 468, 310, 571,  620, 636, 633, 677])
    # model = model.fit(x, y)
    # p = model.named_steps['linear'].coef_
    # p /= p[0]
    # print(p)
