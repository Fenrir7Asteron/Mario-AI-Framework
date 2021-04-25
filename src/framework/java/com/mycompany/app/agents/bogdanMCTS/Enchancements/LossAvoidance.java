package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.SimulationResult;
import com.mycompany.app.agents.bogdanMCTS.Utils;

import java.util.LinkedList;

public class LossAvoidance {
    private static final int NUMBER_OF_MOVES_REPLACE = 2;

    public static SimulationResult AvoidLoss(LinkedList<Integer> moveHistory, TreeNode sourceNode, int currentDepth) {
        var sourceSnapshot = sourceNode.getSceneSnapshot();

        // Create another simulation node and advance it until one move before the loss.
        TreeNode lossAvoidingSimulationNode = NodeBuilder.allocateNode(
                -1,
                null,
                sourceNode.getTree(),
                sourceSnapshot.clone());

        int loseAction = -1;
        int numberOfMovesToReplace = 0;

        while (numberOfMovesToReplace < moveHistory.size()
                && numberOfMovesToReplace < NUMBER_OF_MOVES_REPLACE) {
            loseAction = moveHistory.removeLast();
            numberOfMovesToReplace++;
        }
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
            if (moveId == loseAction) {
                continue;
            }

            TreeNode nodeVariant = NodeBuilder.allocateNode(
                    -1,
                    null,
                    sourceNode.getTree(),
                    lossAvoidingSimulationNode.getSceneSnapshot().clone());

            nodeVariant.makeMove(Utils.availableActions[moveId], numberOfMovesToReplace);
            double reward = Utils.calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), currentDepth);
            if (reward > maxReward) {
                maxReward = reward;
                bestActionId = moveId;
            }
        }

        moveHistory.add(bestActionId);
        return new SimulationResult(maxReward, moveHistory);
    }
}
