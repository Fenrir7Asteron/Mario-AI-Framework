package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

import java.util.Random;
import java.util.Set;

public class MCTree {
    private static final int MAX_DEPTH = 15;
    private static final double EXPLORATION_FACTOR = 0.25f;
    private static final double MIXMAX_MAX_FACTOR = 1.0f;
    private static final double REWARD_DISCOUNT_FACTOR = 1.0f;
    private TreeNode root = null;
    private Random random = null;
    private int repetitions = 1;
    private Set<Enhancement> enhancements;
    public int depth;

    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
    }

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements, Random random) {
        this.repetitions = repetitions;
        this.enhancements = enhancements;
        this.random = random;
        root = new TreeNode(-1, null, repetitions, random, null);
        root.initializeRoot(model);
        if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
            root.expandAll();
        } else {
            root.expandOne();
        }
        depth = 0;
    }

    boolean[] search(MarioTimer timer) {
//        int count = 0;
        while (timer.getRemainingTime() > 0) {
//            ++count;
            TreeNode nodeSelected = selectNode();
            nodeSelected = expandIfNeeded(nodeSelected);
            double reward = simulate(nodeSelected);
            backpropagate(nodeSelected, reward);
        }

        TreeNode bestNode = bestChild(root);
        root = bestNode;
        root.parent = null;
        return bestNode.action;
    }

    void updateModel(MarioForwardModel model) {
        root.sceneSnapshot = model;
        root.snapshotVersion++;
    }

    private TreeNode expandIfNeeded(TreeNode node) {
        if (node.parent != null && node.snapshotVersion != node.parent.snapshotVersion) {
            return node;
        }
        if (node.visitCount > 0 && node.children.size() < Utils.availableActions.length) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                node = node.expandOne();
            } else {
                node = node.expandAll();
            }
//            depth = Math.max(depth, node.depth) - root.depth;
        }
        return node;
    }

    private TreeNode selectNode() {
        TreeNode current = root;
        while (!current.isLeaf() && current.snapshotVersion == root.snapshotVersion) {
            TreeNode next = bestChild(current);
            double maxConfidence = calcConfidence(next);
            int n = current.visitCount;
            int expands = current.children.size();
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION) && current.children.size() < Utils.availableActions.length) {
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
            return 0;
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
        double exploration = EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / nj);
        double conf = exploitation + exploration;
//        System.out.println(conf + " " + exploitation + " " + n + " " + nj);
        return conf;
    }

    private double simulate(TreeNode selectedNode) {
        selectedNode.simulatePos();
        TreeNode simulationNode = new TreeNode(-1, null, 1, random, null);
        simulationNode.sceneSnapshot = selectedNode.sceneSnapshot.clone();
        int step = 0;
        while (step < MAX_DEPTH && !simulationNode.isGameOver()) {
            ++step;
            simulationNode.randomMove();
        }

        // Return reward after random simulation
        double reward;
        if (simulationNode.isGameOver()) {
            if (simulationNode.sceneSnapshot.getGameStatus() == GameStatus.WIN) {
                reward = 1.0f;
            } else {
                reward = 0.0f;
            }
        } else {
            reward = calcReward(simulationNode.sceneSnapshot.getMarioFloatPos()[0],
                    selectedNode.sceneSnapshot.getMarioFloatPos()[0]);
        }
        return reward;
    }

    private double calcReward(double xend, double xstart) {
        return 0.5 + 0.5 * (xend - xstart) / (11 * (1 + MAX_DEPTH));
    }

    private void backpropagate(TreeNode currentNode, double reward) {
        while (!currentNode.isRoot()) {
            currentNode.updateReward(reward);
//            double parentReward = calcReward(currentNode.sceneSnapshot.getMarioFloatPos()[0],
//                    currentNode.parent.sceneSnapshot.getMarioFloatPos()[0]);
//            reward = reward * REWARD_DISCOUNT_FACTOR;
            currentNode = currentNode.parent;
        }
        root.updateReward(reward);
    }
}
