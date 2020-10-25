package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.Random;
import java.util.Set;

public class MCTree {
    private static final int MAX_DEPTH = 20;
    private static final double EXPLORATION_FACTOR = 0.25f;
    private static final double MIXMAX_MAX_FACTOR = 1.0f;
    private TreeNode root = null;
    private Random random = null;
    private int repetitions = 1;
    private Set<Enhancement> enhancements;

    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
    }

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements, Random random) {
        this.repetitions = repetitions;
        this.enhancements = enhancements;
        this.random = random;
        root = new TreeNode(-1, null, repetitions, MAX_DEPTH, random, null);
        root.initializeRoot(model);
        if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            root.expandAll();
        } else {
            root.expandOne();
        }
    }

    boolean[] search(MarioTimer timer) {
        while (timer.getRemainingTime() > 0) {
            TreeNode nodeSelected = selectNode();
            nodeSelected = expandIfNeeded(nodeSelected);
            double reward = simulate(nodeSelected);
            backpropagate(nodeSelected, reward);
        }

        TreeNode bestNode = bestChild(root);
        root = bestNode;
        root.parent = null;
//        System.out.println(bestNode.visitCount);
        return bestNode.action;
    }

    void updateModel(MarioForwardModel model) {
        root.sceneSnapshot = model;
    }

    private TreeNode expandIfNeeded(TreeNode node) {
        if (node.visitCount > 0) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                node = node.expandOne();
            } else {
                node = node.expandAll();
            }
        }
        return node;
    }

    private TreeNode selectNode() {
        TreeNode current = root;
        while (!current.isLeaf()) {
            TreeNode next = bestChild(current);
            double maxConfidence = calcConfidence(next);
            int n = current.visitCount;
            int expands = current.children.size();
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION) && current.children.size() < MarioActions.numberOfActions()) {
                double unexploredConf = 0.5 + EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / (1 + expands));
                if (expands == 0 || unexploredConf > maxConfidence) {
                    return current;
                }
            }

            current = next;
        }

        return current;
    }

    private TreeNode bestChild(TreeNode current) {
        double maxConfidence = Double.NEGATIVE_INFINITY;
        TreeNode best = null;
        for (TreeNode child : current.children) {
            double conf = calcConfidence(child);
            if (conf > maxConfidence) {
                maxConfidence = conf;
                best = child;
            }
        }

        return best;
    }

    private double calcConfidence(TreeNode node) {
        if (node == null) {
            return Double.NEGATIVE_INFINITY;
        }
        int n = node.parent.visitCount;
        int nj = node.visitCount;
        if (nj == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double exploitation;
        if (enhancements.contains(Enhancement.MIXMAX)) {
            exploitation = MIXMAX_MAX_FACTOR * node.maxReward + (1.0f - MIXMAX_MAX_FACTOR) * node.averageReward;
        } else {
            exploitation = node.averageReward;
        }
        double conf = exploitation + EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / nj);
//        System.out.println(conf + " " + exploitation + " " + n + " " + nj);
        return conf;
    }

    private double simulate(TreeNode selectedNode) {
        selectedNode.simulatePos();
        TreeNode simulationNode = new TreeNode(-1, null, 1, MAX_DEPTH, random, null);
        simulationNode.sceneSnapshot = selectedNode.sceneSnapshot.clone();
        int step = 0;
        while (step < MAX_DEPTH && !selectedNode.isGameOver()) {
            ++step;
            simulationNode.randomMove();
        }

        // Return reward after random simulation
        if (simulationNode.isGameOver()) {
            if (simulationNode.sceneSnapshot.getGameStatus() == GameStatus.WIN) {
                return 1.0f;
            } else {
                return -1.0f;
            }
        }
        float xj = simulationNode.sceneSnapshot.getMarioFloatPos()[0];
        float xp = selectedNode.sceneSnapshot.getMarioFloatPos()[0];
        return 0.5 + 0.5 * (xj - xp) / (11 * (1 + MAX_DEPTH));
    }

    private void backpropagate(TreeNode currentNode, double reward) {
        while (!currentNode.isRoot()) {
            currentNode.updateReward(reward);
            currentNode = currentNode.parent;
        }
        root.updateReward(reward);
    }
}
