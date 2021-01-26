package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.HardPruning;
import com.mycompany.app.agents.bogdanMCTS.Enchancements.SafetyPrepruning;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodePool;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.utils.RNG;
import com.mycompany.app.utils.ThreadPool;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.engine.helper.GameStatus;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MCTree implements Cloneable {

    public static final double PROGRESS_WEIGHT = 0.5;
    public static final double DAMAGE_WEIGHT = 0.5;
    public static final double PATH_LENGTH_WEIGHT = 0.5;
    public static final float MAX_REWARD = 1.0f;
    public static final float MIN_REWARD = 0.0f;

    public final static int MAX_SIMULATION_DEPTH = 6;
    public final static double EXPLORATION_FACTOR = 0.188f;
    public final static boolean DETERMINISTIC = false;
    public final static int SEARCH_REPETITIONS = 100;
    static int repetitions = 1;
    static Set<Enhancement> enhancements;

    private TreeNode root = null;
    private int maxTreeDepth;

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements) {
        MCTree.repetitions = repetitions;
        MCTree.enhancements = enhancements;
        initializeRoot(model);
        maxTreeDepth = 0;
    }

    public static double getExplorationFactor() {
        return EXPLORATION_FACTOR;
    }

    public static int getMaxSimulationDepth() {
        return MAX_SIMULATION_DEPTH;
    }

    public static int getRepetitions() {
        return repetitions;
    }

    public static Set<Enhancement> getEnhancements() {
        return enhancements;
    }

    public void initializeRoot(MarioForwardModel model) {
        root = NodePool.allocateNode(-1, null, model.clone());
        if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            root.expandAll();
        } else {
            root.expandOne();
        }
    }

    public boolean[] search(MarioTimer timer) {
        int count = 0;

        if (MCTree.enhancements.contains(Enhancement.SAFETY_PREPRUNING)) {
            SafetyPrepruning.safetyPreprune(root);
        }

        if (DETERMINISTIC) {
            while (count < SEARCH_REPETITIONS) {
                ++count;
                TreeNode nodeSelected = selectAndExpand();
                double reward = simulate(nodeSelected);
                backpropagate(nodeSelected, reward);
            }
        } else {
            while (timer.getRemainingTime() > 0) {
                ++count;
                TreeNode nodeSelected = selectAndExpand();
                double reward = simulate(nodeSelected);
                backpropagate(nodeSelected, reward);
            }
        }

        TreeNode bestNode = root.getBestChild(false);
        int bestActionId = bestNode.getActionId();
        if (!MCTree.enhancements.contains(Enhancement.TREE_REUSE)) {
            clearTree();
        } else {
            // By detaching the best node from tree it and it's subtree are not cleared.
            bestNode.detachFromTree();
            clearTree();
            root = bestNode;
            maxTreeDepth = root.getMaxSubTreeDepth();
        }
        return Utils.availableActions[bestActionId];
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        MCTree cloned = (MCTree) super.clone();
        if (cloned.root != null) {
            cloned.root = (TreeNode) cloned.root.clone();
        }
        return cloned;
    }

    private void clearTree() {
        root.clearSubTree();
        root = null;
    }


    boolean checkTreeRoot() {
        return root != null;
    }

    void updateModel(MarioForwardModel model) {
        if (!checkTreeRoot()) {
            initializeRoot(model);
        } else {
            root.setSceneSnapshot(model);
        }
    }

    private TreeNode expandIfNeeded(TreeNode node) {
        if (node.getVisitCount() > 0 && node.getChildrenSize() < Utils.availableActions.length) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION) && node.getChildrenSize() == 0) {
                node = node.expandOne();
            } else if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                node = node.expandAll();
            }
            maxTreeDepth = Math.max(maxTreeDepth, node.getDepth());
        }
        return node;
    }

    private TreeNode selectAndExpand() {
        TreeNode current = root;
        while (!current.isLeaf()) {
            if (current.getParent() != null && current.getSnapshotVersion() != current.getParent().getSnapshotVersion()) {
                current.poolSnapshotFromParent();
            }
            TreeNode next = current.getBestChild(true);
            int n = current.getVisitCount();
            int expands = current.getChildrenSize();
            if (n > 0 && enhancements.contains(Enhancement.PARTIAL_EXPANSION) && current.getChildrenSize() < Utils.availableActions.length) {
                double unexploredConf = 0.5 + EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / (1 + expands));
                if (expands == 0 || unexploredConf > current.getMaxConfidence()) {
                    return current.expandOne();
                }
            }

            current = next;
        }

        return expandIfNeeded(current);
    }

    private double simulate(TreeNode sourceNode) {
        sourceNode.simulatePos();

        if (sourceNode.isLost()) {
            sourceNode.prune();
            return MIN_REWARD;
        }

        var sourceSnapshot = sourceNode.getSceneSnapshot();

        TreeNode simulationNode = NodePool.allocateNode(-1, null, sourceSnapshot.clone());

        int step = 0;
        ArrayList<boolean[]> moveHistory = new ArrayList<>();

        while ((!simulationNode.isLost() || !simulationNode.isWin()) && step < MAX_SIMULATION_DEPTH) {
            ++step;

            var nextMove = simulationNode.getRandomMove();
            simulationNode.makeMove(nextMove);

            if (enhancements.contains(Enhancement.LOSS_AVOIDANCE)) {
                moveHistory.add(nextMove);
            }
        }

        if (simulationNode.isLost()) {
            if (enhancements.contains(Enhancement.LOSS_AVOIDANCE)) {
                // Create another simulation node and advance it until one move before the loss.
                TreeNode lossAvoidingSimulationNode = NodePool.allocateNode(-1, null,
                        sourceSnapshot.clone());
                lossAvoidingSimulationNode.makeMoves(moveHistory);

                List<boolean[]> availableMoves = lossAvoidingSimulationNode.getAllMoves();
                double maxReward = MIN_REWARD;

                // Try all available moves and return the best result.
                // During the Loss Avoidance max simulation depth is ignored for the optimization purposes.
                ArrayList<Future<Double>> futureRewards = new ArrayList<>();

                for (var moveVariant : availableMoves) {
                    futureRewards.add(ThreadPool.nodeCalculationsThreadPool.submit(() -> {
                        TreeNode nodeVariant = NodePool.allocateNode(-1, null,
                                lossAvoidingSimulationNode.getSceneSnapshot().clone());
                        nodeVariant.makeMove(moveVariant);
                        return calcReward(sourceSnapshot, nodeVariant.getSceneSnapshot(), sourceNode.getDepth());
                    }));
                }

                for (var futureReward : futureRewards) {
                    try {
                        maxReward = Math.max(maxReward, futureReward.get());
                    } catch (InterruptedException | ExecutionException ex) {
                        ex.printStackTrace();
                    }
                }

                return maxReward;
            } else {
                return MIN_REWARD;
            }
        }

        if (simulationNode.isWin()) {
            return MAX_REWARD;
        }

        // Return reward at the end of a simulation.
        return calcReward(sourceSnapshot, simulationNode.getSceneSnapshot(), sourceNode.getDepth());
    }

    private double calcReward(MarioForwardModel startSnapshot, MarioForwardModel endSnapshot, int currentDepth) {
        if (endSnapshot.getGameStatus() != GameStatus.RUNNING) {
            // If it is Game Over, there is either win or lose.
            return endSnapshot.getGameStatus() == GameStatus.WIN ? MAX_REWARD : MIN_REWARD;
        }

        double startX = startSnapshot.getMarioFloatPos()[0];
        double endX = endSnapshot.getMarioFloatPos()[0];
        int damage = Math.max(0, startSnapshot.getMarioMode() - endSnapshot.getMarioMode()) +
                Math.max(0, startSnapshot.getNumLives() - endSnapshot.getNumLives());

        double reward = 0.5 +
                PROGRESS_WEIGHT * (endX - startX) / (11.0 * (1 + MAX_SIMULATION_DEPTH))
//                        + PATH_LENGTH_WEIGHT * (maxTreeDepth - currentDepth) / maxTreeDepth
                        - DAMAGE_WEIGHT * damage;

        return reward;
    }

    private void backpropagate(TreeNode currentNode, double reward) {
        while (currentNode != null) {
            currentNode.updateReward(reward);
            if (MCTree.getEnhancements().contains(Enhancement.HARD_PRUNING)) {
                HardPruning.tryPruneChildren(currentNode);
            }
            currentNode = currentNode.getParent();
        }
    }

    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
        LOSS_AVOIDANCE,
        TREE_REUSE,
        HARD_PRUNING,
        SAFETY_PREPRUNING,
    }
}
