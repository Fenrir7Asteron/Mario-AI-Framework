package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.stream.Collectors;

public class HardPruning {
    private final static int MIN_VISITS = 100;
    private final static int MIN_REMAINING_CHILDREN = 4;
    private final static double PRUNING_SPEED = 1.0f;

    public static void tryPruneChildren(TreeNode parent) {
        if (parent.getVisitCountComplete() > MIN_VISITS) {
            var sortedChildren = (ArrayList<TreeNode>) parent.getChildren().clone();
            sortedChildren.sort(Comparator.comparingDouble(TreeNode::getAverageReward));
//            var sortedChildren = parent.getChildren().stream()
//                    .sorted()
//                    .collect(Collectors.toList());

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
