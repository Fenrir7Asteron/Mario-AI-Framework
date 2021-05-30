package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.*;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.NodeBuilder;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.utils.Constants;

import java.util.*;

public class MCTree implements Cloneable {
    public static double PROGRESS_WEIGHT = 0.5;
    public static double BASE_REWARD = 0.5;
    public static double DAMAGE_WEIGHT = 0.5;
    public static double AGE_DECAY = 0.02;
    public static float MAX_REWARD = 1.0f;
    public static float MIN_REWARD = 0.0f;

    public static int MAX_TREE_DEPTH = 30;
    public static int MAX_SIMULATION_DEPTH = 12;
    public static double EXPLORATION_FACTOR = 0.188f;
    public static boolean DETERMINISTIC = false;
    public static int SEARCH_REPETITIONS = 100;
    public static int MACRO_REPETITIONS = 3;
    public static int DANGER_NODE_DISTANCE = 3;

    int enhancements;

    public final WU_UCT wuUct;
    public final NGramSelection nGramSelection;
    public final HardPruning hardPruning;
    public final LossAvoidance lossAvoidance;
    public final MixMax mixMax;
    public final PartialExpansion partialExpansion;
    public final SafetyPrepruning safetyPrepruning;
    public final SP_MCTS spMCTS;


    private TreeNode _root = null;
    private int actionRepeated;
    private int bestActionId;

    MCTree(MarioForwardModel model) {
        initializeRoot(model);
        hardPruning = new HardPruning();
        lossAvoidance = new LossAvoidance();
        mixMax = new MixMax();
        nGramSelection = new NGramSelection();
        partialExpansion = new PartialExpansion();
        safetyPrepruning = new SafetyPrepruning();
        spMCTS = new SP_MCTS();
        wuUct = new WU_UCT();

        actionRepeated = getRepetitions();
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
        if (MCTSEnhancements.MaskContainsEnhancement(enhancements, MCTSEnhancements.Enhancement.MACRO_ACTIONS)) {
            return MACRO_REPETITIONS;
        } else {
            return 1;
        }
    }

    public int getEnhancements() {
        return enhancements;
    }

    public void setEnhancements(int enhancementMask) {
        enhancements = enhancementMask;
    }

    public NGramSelection getNGramSelection() {
        return nGramSelection;
    }

    public void initializeRoot(MarioForwardModel model) {
        if (model == null) {
            _root = null;
            return;
        }

        int repetitions = getRepetitions();
        _root = NodeBuilder.allocateNode(-1, null, this, model.clone(), repetitions);

        if (!MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
            _root.expandAll(repetitions);
        } else {
            _root.expandOne(repetitions);
        }
    }

    public boolean[] search(MarioTimer timer) {
        monteCarloUpdates(timer);

        if (_root.getDangerous() || !MCTSEnhancements.MaskContainsEnhancement(enhancements, MCTSEnhancements.Enhancement.MACRO_ACTIONS)) {
            updateTreeRoot();
            return Utils.availableActions[bestActionId];
        }

        if (actionRepeated >= _root.getRepetitions()) {
            updateTreeRoot();
            actionRepeated = 0;
        }

        actionRepeated++;

        return Utils.availableActions[bestActionId];
    }

    private void updateTreeRoot() {
        TreeNode bestNode = getBestNodeAndUpdateBestAction();

        if (_root.getDangerous() || !MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
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
    }

    private TreeNode getBestNodeAndUpdateBestAction() {
        TreeNode bestNode = _root.getBestChild(false);

        if (bestNode != null) {
            bestActionId = bestNode.getActionId();
        } else {
            bestActionId = _root.getRandomMove();
        }

        return bestNode;
    }

    private void monteCarloUpdates(MarioTimer timer) {
        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.SAFETY_PREPRUNING)) {
            safetyPrepruning.safetyPreprune(_root);
        }

        if (DETERMINISTIC) {
            int count = 0;

            while (count < SEARCH_REPETITIONS) {
                if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                        MCTSEnhancements.Enhancement.WU_UCT)) {
                    wuUct.makeOneSearchStep(this);
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
                    wuUct.makeOneSearchStep(this);
                } else {
                    makeOneSearchStep(_root);
                }
            }
        }

        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.WU_UCT)) {
            wuUct.clear(this);
        }

        if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
            nGramSelection.decayMoves();
        }
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
                return partialExpansion.isItPartialExpandTime(node);
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
            newNode = node.expandOne(getRepetitions());
        } else {
            newNode = node.expandAll(getRepetitions());
        }

        return newNode;
    }

    public TreeNode select(TreeNode root) {
        TreeNode current = root;
        while (!current.isLeaf()) {
            TreeNode next = current.getBestChild(true);

            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
                if (partialExpansion.isItPartialExpandTime(current)) {
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

        TreeNode simulationNode = NodeBuilder.allocateNode(-1, null, this, sourceSnapshot.clone(), 1);

        int step = 0;
        LinkedList<Integer> moveHistory = new LinkedList<>();

        while ((!simulationNode.isLost() || !simulationNode.isWin()) && step < MAX_SIMULATION_DEPTH) {
            ++step;

            int nextMoveId;
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
                nextMoveId = nGramSelection.getMove(moveHistory);
            } else {
                nextMoveId = simulationNode.getRandomMove();
            }

            simulationNode.makeMove(Utils.availableActions[nextMoveId], 1);
            moveHistory.add(nextMoveId);
        }

        if (simulationNode.isLost()) {
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.LOSS_AVOIDANCE)) {
                // Try find a sibling simulation node with minimal loss.
                return lossAvoidance.AvoidLoss(moveHistory, sourceNode, sourceNode.getDepth());
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
        int distanceFromLeaf = 0;

        while (currentNode != null) {
            if (MCTSEnhancements.MaskContainsEnhancement(enhancements, MCTSEnhancements.Enhancement.MACRO_ACTIONS)
                    && distanceFromLeaf < DANGER_NODE_DISTANCE
                    && reward < MCTree.MIN_REWARD + Constants.EPSILON) {
                distanceFromLeaf++;
                currentNode.setDangerous(true);
            }

            currentNode.updateReward(reward);
            if (MCTSEnhancements.MaskContainsEnhancement(getEnhancements(),
                    MCTSEnhancements.Enhancement.HARD_PRUNING)) {
                hardPruning.tryPruneChildren(currentNode);
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
            nGramSelection.updateRewards(result.moveHistory, result.reward);
        }
        backpropagate(node, result.reward);
    }
}
