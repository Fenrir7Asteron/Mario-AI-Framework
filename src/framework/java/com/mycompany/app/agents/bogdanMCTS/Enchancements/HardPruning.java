package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;

import java.util.Comparator;
import java.util.stream.Collectors;

public class HardPruning {
    private final static int MIN_VISITS = 50;
    private final static int MIN_REMAINING_CHILDREN = 4;
    private final static double PRUNING_SPEED = 2.0f;

    public static void tryPruneChildren(TreeNode parent) {
        if (parent.getVisitCount() > MIN_VISITS) {
            var sortedChildren = parent.getChildren().stream()
                    .sorted(Comparator.comparingDouble(TreeNode::getAverageReward))
                    .collect(Collectors.toList());

            int availableToPrune = sortedChildren.size() - MIN_REMAINING_CHILDREN;
            int pruneCount = (int) (PRUNING_SPEED * Math.log(availableToPrune));
            for (int i = 0; i < Math.min(pruneCount, availableToPrune); ++i) {
                var child = sortedChildren.get(i);

                if (!child.isPruned()) {
                    child.prune();
                }
            }
        }
    }
}
