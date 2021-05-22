package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.utils.Constants;

public class SP_MCTS {
    public static double D = 4;

    public double variability(TreeNode node) {
        double averageRewardSquared = node.getAverageReward() * node.getAverageReward();
        float visitCount = node.getVisitCount();

        if (Math.abs(visitCount) < Constants.EPSILON) {
            return Double.POSITIVE_INFINITY;
        }

        return Math.sqrt((node.getSumOfSquaredRewards() - visitCount * averageRewardSquared + D) / visitCount);
    }
}