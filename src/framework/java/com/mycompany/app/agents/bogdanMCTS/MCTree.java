package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.HardPruning;
import com.mycompany.app.agents.bogdanMCTS.Enchancements.LossAvoidance;
import com.mycompany.app.agents.bogdanMCTS.Enchancements.SafetyPrepruning;
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

    public final static int MAX_SIMULATION_DEPTH = 6;
    public final static double EXPLORATION_FACTOR = 0.188f;
    public final static boolean DETERMINISTIC = false;
    public final static int SEARCH_REPETITIONS = 100;
    static int repetitions = 1;
    static Set<Enhancement> enhancements;

    private TreeNode _root = null;
    private int _maxTreeDepth;
    private boolean _needExpand = false;

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements) {
        MCTree.repetitions = repetitions;
        MCTree.enhancements = enhancements;
        initializeRoot(model);
        _maxTreeDepth = 0;
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
        int count = 0;

        if (MCTree.enhancements.contains(Enhancement.SAFETY_PREPRUNING)) {
            SafetyPrepruning.safetyPreprune(_root);
        }

        if (DETERMINISTIC) {
            while (count < SEARCH_REPETITIONS) {
                ++count;
                TreeNode nodeSelected = select();
                double reward = simulate(nodeSelected);
                backpropagate(nodeSelected, reward);
            }
        } else {
            while (timer.getRemainingTime() > 0) {
                ++count;

                TreeNode node = select();
                if (_needExpand) {
                    node = expand(node);
                }
                double reward = simulate(node);
                backpropagate(node, reward);
            }
        }

        TreeNode bestNode = _root.getBestChild(false);
        int bestActionId = bestNode.getActionId();
        if (!MCTree.enhancements.contains(Enhancement.TREE_REUSE)) {
            clearTree();
        } else {
            // By detaching the best node from tree it and it's subtree are not cleared.
            bestNode.detachFromTree();
            clearTree();
            _root = bestNode;
            _maxTreeDepth = _root.getMaxSubTreeDepth();
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

    private void clearTree() {
        _root.clearSubTree();
        _root = null;
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

    private boolean isExpandNeeded(TreeNode node) {
        if (node.getVisitCount() > 0 && node.getChildrenSize() < Utils.availableActions.length) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                // If we can expand partially, then do it only if there are no expanded children yet.
                return node.getChildrenSize() == 0;
            } else {
                return true;
            }
        }
        return false;
    }

    private TreeNode expand(TreeNode node) {
        TreeNode newNode;

        if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            newNode = node.expandOne();
        } else {
            newNode = node.expandAll();
        }

        _maxTreeDepth = Math.max(_maxTreeDepth, newNode.getDepth());
        _needExpand = false;

        return newNode;
    }

    private TreeNode select() {
        TreeNode current = _root;
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
                    _needExpand = true;
                    return current;
                }
            }

            current = next;
        }

        _needExpand = isExpandNeeded(current);
        return current;
    }

    private double simulate(TreeNode sourceNode) {
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
