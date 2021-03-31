package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.ThreadPool;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LossAvoidance {
    public static MCTree.SimulationResult AvoidLoss(LinkedList<Integer> moveHistory, TreeNode sourceNode, int currentDepth) {
        var sourceSnapshot = sourceNode.getSceneSnapshot();

        // Create another simulation node and advance it until one move before the loss.
        TreeNode lossAvoidingSimulationNode = NodeBuilder.allocateNode(
                -1,
                null,
                sourceNode.getTree(),
                sourceSnapshot.clone());
        moveHistory.removeLast();
        lossAvoidingSimulationNode.makeMoves(moveHistory);

        double maxReward = MCTree.MIN_REWARD - 1;
        int bestActionId = -1;

        // Try all available moves and return the best result.
        // During the Loss Avoidance max simulation depth is ignored for the optimization purposes.
//        ArrayList<Future<Double>> futureRewards = new ArrayList<>();
//
//        for (var moveVariant : availableMoves) {
//            futureRewards.add(ThreadPool.nodeCalculationsThreadPool.submit(() -> {
//                TreeNode nodeVariant = NodeBuilder.allocateNode(-1, null,
//                        lossAvoidingSimulationNode.getSceneSnapshot().clone());
//                nodeVariant.makeMove(moveVariant);
//                return Utils.calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), currentDepth);
//            }));
//        }
//
//        for (var futureReward : futureRewards) {
//            try {
//
//            } catch (InterruptedException | ExecutionException ex) {
//                ex.printStackTrace();
//            }
//        }

        for (int moveId = 0; moveId < Utils.availableActions.length; ++moveId) {
            TreeNode nodeVariant = NodeBuilder.allocateNode(
                    -1,
                    null,
                    sourceNode.getTree(),
                    lossAvoidingSimulationNode.getSceneSnapshot().clone());
            nodeVariant.makeMove(Utils.availableActions[moveId]);
            double reward = Utils.calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), currentDepth);
            if (reward > maxReward) {
                reward = maxReward;
                bestActionId = moveId;
            }
        }

        moveHistory.add(bestActionId);
        return new MCTree.SimulationResult(maxReward, moveHistory);
    }
}
