package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.Utils;

public class PartialExpansion {
    public static boolean isItPartialExpandTime(TreeNode node) {
        int n = node.getVisitCount();
        int expands = node.getChildrenSize();

        double unexploredConf = 0.5 + MCTree.EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / (1 + expands));

        if (n > 0 && node.getChildrenSize() < Utils.availableActions.length) {
            return expands == 0 || unexploredConf > node.getMaxConfidence();
        }

        return false;
    }
}
