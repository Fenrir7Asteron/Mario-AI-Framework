package agents.bogdanMCTS.Enchancements;

import agents.bogdanMCTS.NodeInternals.TreeNode;
import agents.bogdanMCTS.Workers.ThreadPool;

import java.beans.IntrospectionException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class SafetyPrepruning {
    public static void safetyPreprune(TreeNode root) {
        root.expandAll();

        ArrayList<Future<?>> futures = new ArrayList<>();

        for (var child : root.getChildren()) {
            futures.add(ThreadPool.threadPool.submit(() -> {
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
