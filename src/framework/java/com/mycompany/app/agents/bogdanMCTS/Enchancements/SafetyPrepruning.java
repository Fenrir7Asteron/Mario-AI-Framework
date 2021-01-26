package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.utils.ThreadPool;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SafetyPrepruning {
    public static void safetyPreprune(TreeNode root) {
        root.expandAll();

        ArrayList<Future<?>> futures = new ArrayList<>();

        for (var child : root.getChildren()) {
            futures.add(ThreadPool.nodeCalculationsThreadPool.submit(() -> {
                child.simulatePos();
                if (child.isLost()) {
                    child.prune();
                }
            }));
        }

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
            }
        }
    }
}
