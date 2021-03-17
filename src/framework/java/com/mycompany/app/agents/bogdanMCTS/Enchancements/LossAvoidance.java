package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.ThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class LossAvoidance {
    public static double AvoidLoss(ArrayList<boolean[]> moveHistory, TreeNode sourceNode) {
        var sourceSnapshot = sourceNode.getSceneSnapshot();

        // Create another simulation node and advance it until one move before the loss.
        TreeNode lossAvoidingSimulationNode = NodeBuilder.allocateNode(-1, null,
                sourceSnapshot.clone());
        lossAvoidingSimulationNode.makeMoves(moveHistory);

        List<boolean[]> availableMoves = lossAvoidingSimulationNode.getAllMoves();
        double maxReward = MCTree.MIN_REWARD;

        // Try all available moves and return the best result.
        // During the Loss Avoidance max simulation depth is ignored for the optimization purposes.
//        ArrayList<Future<Double>> futureRewards = new ArrayList<>();
//
//        for (var moveVariant : availableMoves) {
//            futureRewards.add(ThreadPool.nodeCalculationsThreadPool.submit(() -> {
//                TreeNode nodeVariant = NodeBuilder.allocateNode(-1, null,
//                        lossAvoidingSimulationNode.getSceneSnapshot().clone());
//                nodeVariant.makeMove(moveVariant);
//                return Utils.calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), sourceNode.getDepth());
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

        for (var moveVariant : availableMoves) {
            TreeNode nodeVariant = NodeBuilder.allocateNode(-1, null,
            lossAvoidingSimulationNode.getSceneSnapshot().clone());
            nodeVariant.makeMove(moveVariant);
            maxReward = Math.max(
                    maxReward,
                    Utils.calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), sourceNode.getDepth())
            );
        }

        return maxReward;
    }
}
