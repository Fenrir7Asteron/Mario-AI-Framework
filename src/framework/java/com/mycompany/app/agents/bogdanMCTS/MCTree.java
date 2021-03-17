package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.*;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;

import java.util.*;

public class MCTree implements Cloneable {

    public static final double PROGRESS_WEIGHT = 0.5;
    public static final double DAMAGE_WEIGHT = 0.5;
    public static final double PATH_LENGTH_WEIGHT = 0.5;
    public static final float MAX_REWARD = 1.0f;
    public static final float MIN_REWARD = 0.0f;

    public final static int MAX_SIMULATION_DEPTH = 15;
    public final static double EXPLORATION_FACTOR = 0.188f;
    public final static boolean DETERMINISTIC = false;
    public final static int SEARCH_REPETITIONS = 100;
    static int repetitions = 1;
    static Set<Enhancement> enhancements;

    private TreeNode _root = null;

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements) {
        MCTree.repetitions = repetitions;
        MCTree.enhancements = enhancements;
        initializeRoot(model);
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
        _root = NodeBuilder.allocateNode(-1, null, model.clone());

        if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            _root.expandAll();
        } else {
            _root.expandOne();
        }
    }

    public boolean[] search(MarioTimer timer) {
        if (MCTree.enhancements.contains(Enhancement.SAFETY_PREPRUNING)) {
            SafetyPrepruning.safetyPreprune(_root);
        }

        if (DETERMINISTIC) {
            int count = 0;

            while (count < SEARCH_REPETITIONS) {
                if (enhancements.contains(Enhancement.WU_UCT)) {
                    WU_UCT.makeOneSearchStep(_root);
                } else {
                    makeOneSearchStep(_root);
                }
                ++count;
            }
        } else {
            while (timer.getRemainingTime() > 0) {
                if (enhancements.contains(Enhancement.WU_UCT)) {
                    WU_UCT.makeOneSearchStep(_root);
                } else {
                    makeOneSearchStep(_root);
                }
            }
        }

        if (enhancements.contains(Enhancement.WU_UCT)) {
            WU_UCT.clear();
        }

        TreeNode bestNode = _root.getBestChild(false);
//        System.out.println(_root.getMaxSubTreeDepth());
//        System.out.println(_root.getVisitCount());
//        System.out.println(bestNode.getVisitCount());
        int bestActionId = bestNode.getActionId();
        if (!MCTree.enhancements.contains(Enhancement.TREE_REUSE)) {
            _root.clearSubTree();
        } else {
            // By detaching the best node from tree it and it's subtree are not cleared.
            bestNode.detachFromTree();
            _root.clearSubTree();
            _root = bestNode;
        }
        return Utils.availableActions[bestActionId];
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        MCTree cloned = (MCTree) super.clone();
        if (cloned._root != null) {
            cloned._root = (TreeNode) cloned._root.clone();
        }
        return cloned;
    }

    boolean checkTreeRoot() {
        return _root != null;
    }

    void updateModel(MarioForwardModel model) {
        if (!checkTreeRoot()) {
            initializeRoot(model);
        } else {
            _root.setSceneSnapshot(model);
        }
    }

    public static boolean isExpandNeededForSelection(TreeNode node) {
        if (node.getVisitCount() > 0
                && node.getChildrenSize() < Utils.availableActions.length) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                return PartialExpansion.isItPartialExpandTime(node);
            } else {
                return true;
            }
        }

        return false;
    }

    public synchronized static TreeNode expand(TreeNode node) {
        TreeNode newNode;

        if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            newNode = node.expandOne();
        } else {
            newNode = node.expandAll();
        }

        return newNode;
    }

    public static TreeNode select(TreeNode root) {
        TreeNode current = root;
        while (!current.isLeaf()) {
            TreeNode next = current.getBestChild(true);

            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                if (PartialExpansion.isItPartialExpandTime(current)) {
                    return current;
                }
            }

            current = next;
        }

        return current;
    }

    public static double simulate(TreeNode sourceNode) {
        if (sourceNode.isLost()) {
            sourceNode.prune();
            return MIN_REWARD;
        }

        var sourceSnapshot = sourceNode.getSceneSnapshot();

        TreeNode simulationNode = NodeBuilder.allocateNode(-1, null, sourceSnapshot.clone());

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
                // Try find a sibling simulation node with minimal loss.
                return LossAvoidance.AvoidLoss(moveHistory, sourceNode);
            } else {
                return MIN_REWARD;
            }
        }

        if (simulationNode.isWin()) {
            return MAX_REWARD;
        }

        // Return reward at the end of a simulation.
        return Utils.calcReward(sourceSnapshot, simulationNode.getSceneSnapshot(), sourceNode.getDepth());
    }

    public static void backpropagate(TreeNode currentNode, double reward) {
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
        WU_UCT,
    }


    private void makeOneSearchStep(TreeNode root) {
        TreeNode node = select(root);
        if (isExpandNeededForSelection(node)) {
            node = expand(node);
        }
        double reward = simulate(node);
        backpropagate(node, reward);
    }
}
