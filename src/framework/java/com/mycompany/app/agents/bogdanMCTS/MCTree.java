package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.*;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;

import java.util.*;

public class MCTree implements Cloneable {

    public static final double PROGRESS_WEIGHT = 0.5;
    public static final double BASE_REWARD = 0.5;
    public static final double DAMAGE_WEIGHT = 0.5;
    public static final double AGE_DECAY = 0.03;
    public static final double PATH_LENGTH_WEIGHT = 0.5;
    public static final float MAX_REWARD = 1.0f;
    public static final float MIN_REWARD = 0.0f;

    public static final int MAX_TREE_DEPTH = 1000;
    public static final int MAX_SIMULATION_DEPTH = 6;
    public static final double EXPLORATION_FACTOR = 0.188f;
    public static final boolean DETERMINISTIC = false;
    public static final int SEARCH_REPETITIONS = 100;
    public static final int REPETITIONS = 1;

    int enhancements;

    private TreeNode _root = null;

    public final WU_UCT _wuUct;
    public final NGramSelection _nGramSelection;
    public final HardPruning _hardPruning;
    public final LossAvoidance _lossAvoidance;
    public final MixMax _mixMax;
    public final PartialExpansion _partialExpansion;
    public final SafetyPrepruning _safetyPrepruning;
    public final SP_MCTS _spMCTS;

    MCTree(MarioForwardModel model) {
        initializeRoot(model);
        _hardPruning = new HardPruning();
        _lossAvoidance = new LossAvoidance();
        _mixMax = new MixMax();
        _nGramSelection = new NGramSelection();
        _partialExpansion = new PartialExpansion();
        _safetyPrepruning = new SafetyPrepruning();
        _spMCTS = new SP_MCTS();
        _wuUct = new WU_UCT();
    }

    public TreeNode getRoot() {
        return _root;
    }

    public double getExplorationFactor() {
        return EXPLORATION_FACTOR;
    }

    public int getMaxSimulationDepth() {
        return MAX_SIMULATION_DEPTH;
    }

    public int getRepetitions() {
        return REPETITIONS;
    }

    public int getEnhancements() {
        return enhancements;
    }

    public void setEnhancements(int enhancementMask) {
        enhancements = enhancementMask;
    }

    public NGramSelection getNGramSelection() {
        return _nGramSelection;
    }

    public void initializeRoot(MarioForwardModel model) {
        if (model == null) {
            _root = null;
            return;
        }

        _root = NodeBuilder.allocateNode(-1, null, this, model.clone());

        if (!MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
            _root.expandAll();
        } else {
            _root.expandOne();
        }
    }

    public boolean[] search(MarioTimer timer) {
        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.SAFETY_PREPRUNING)) {
            _safetyPrepruning.safetyPreprune(_root);
        }

        if (DETERMINISTIC) {
            int count = 0;

            while (count < SEARCH_REPETITIONS) {
                if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                        MCTSEnhancements.Enhancement.WU_UCT)) {
                    _wuUct.makeOneSearchStep(this);
                } else {
                    makeOneSearchStep(_root);
                }
                ++count;
            }
        } else {
            while (timer.getRemainingTime() > 0) {
                if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                        MCTSEnhancements.Enhancement.WU_UCT)) {
                    if (timer.getRemainingTime() < 3) {
                        break;
                    }
                    _wuUct.makeOneSearchStep(this);
                } else {
                    makeOneSearchStep(_root);
                }
            }
        }

        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.WU_UCT)) {
            _wuUct.clear(this);
        }

        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
            _nGramSelection.decayMoves();
        }

        TreeNode bestNode = _root.getBestChild(false);

        int bestActionId;
        if (bestNode != null) {
            bestActionId = bestNode.getActionId();
        } else {
            bestActionId = _root.getRandomMove();
        }

        if (!MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.TREE_REUSE)) {
            _root.clearSubTree();
        } else {
            if (bestNode != null) {
                // By detaching the best node from tree it and it's subtree are not cleared.
                bestNode.detachFromTree();
                _root.clearSubTree();

                if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                        MCTSEnhancements.Enhancement.AGING)) {
                    bestNode.ageDecaySubtree();
                }
            }

            _root = bestNode;
        }

        return Utils.availableActions[bestActionId];
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

    public boolean isExpandNeededForSelection(TreeNode node) {
        if (node.getVisitCount() > 0
                && node.getChildrenSize() < Utils.availableActions.length
                && node.getDepth() < MCTree.MAX_TREE_DEPTH
        ) {
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
                return _partialExpansion.isItPartialExpandTime(node);
            } else {
                return true;
            }
        }

        return false;
    }

    public TreeNode expand(TreeNode node) {
        TreeNode newNode;

        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
            newNode = node.expandOne();
        } else {
            newNode = node.expandAll();
        }

        return newNode;
    }

    public TreeNode select(TreeNode root) {
        TreeNode current = root;
        while (!current.isLeaf()) {
            TreeNode next = current.getBestChild(true);

            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
                if (_partialExpansion.isItPartialExpandTime(current)) {
                    return current;
                }
            }

            current = next;
        }

        return current;
    }

    public SimulationResult simulate(TreeNode sourceNode) {
        if (sourceNode.isPruned()) {
            return new SimulationResult(MIN_REWARD, new LinkedList<>());
        }

        var sourceSnapshot = sourceNode.getSceneSnapshot();

        TreeNode simulationNode = NodeBuilder.allocateNode(-1, null, this, sourceSnapshot.clone());

        int step = 0;
        LinkedList<Integer> moveHistory = new LinkedList<>();

        while ((!simulationNode.isLost() || !simulationNode.isWin()) && step < MAX_SIMULATION_DEPTH) {
            ++step;

            int nextMoveId;
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
                nextMoveId = _nGramSelection.getMove(moveHistory);
            } else {
                nextMoveId = simulationNode.getRandomMove();
            }

            simulationNode.makeMove(Utils.availableActions[nextMoveId]);
            moveHistory.add(nextMoveId);
        }

        if (simulationNode.isLost()) {
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.LOSS_AVOIDANCE)) {
                // Try find a sibling simulation node with minimal loss.
                return _lossAvoidance.AvoidLoss(moveHistory, sourceNode, sourceNode.getDepth());
            }
        }

        if (simulationNode.isWin()) {
            return new SimulationResult(MAX_REWARD, moveHistory);
        }

        // Return reward at the end of a simulation.
        return new SimulationResult(Utils.calcReward(
                    sourceSnapshot,
                    simulationNode.getSceneSnapshot(),
                    sourceNode.getDepth(),
                sourceNode)
                , moveHistory);
    }

    public void backpropagate(TreeNode currentNode, double reward) {
        while (currentNode != null) {
            currentNode.updateReward(reward);
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.HARD_PRUNING)) {
                _hardPruning.tryPruneChildren(currentNode);
            }

            PruneIfAllChildrenAreHardPruned(currentNode);

            currentNode = currentNode.getParent();
        }
    }

    private void PruneIfAllChildrenAreHardPruned(TreeNode currentNode) {
        if (currentNode.getChildrenSize() < Utils.availableActions.length) {
            return;
        }

        boolean allChildrenArePruned = true;

        for (TreeNode child : currentNode.getChildren()) {
            if (!child.isPruned()) {
                allChildrenArePruned = false;
            }
        }

        if (allChildrenArePruned) {
            currentNode.prune();
        }
    }


    private void makeOneSearchStep(TreeNode root) {
        TreeNode node = select(root);
        if (isExpandNeededForSelection(node)) {
            node = expand(node);
        }
        SimulationResult result = simulate(node);
        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
            _nGramSelection.updateRewards(result.moveHistory, result.reward);
        }
        backpropagate(node, result.reward);
    }
}
