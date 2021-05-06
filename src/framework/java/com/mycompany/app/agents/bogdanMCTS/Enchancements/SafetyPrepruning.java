package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.utils.ThreadPool;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SafetyPrepruning {
    public void safetyPreprune(TreeNode root) {
        root.expandAll(1);

        for (var child : root.getChildren()) {
            if (child.isLost()) {
                child.prune();
            }
        }
    }
}
