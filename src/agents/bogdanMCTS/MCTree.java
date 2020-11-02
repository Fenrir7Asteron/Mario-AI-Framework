package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

import java.util.Random;
import java.util.Set;

public class MCTree {
    private static final int MAX_DEPTH = 6;
    private static final double EXPLORATION_FACTOR = 0.25f;
    private static final double MIXMAX_MAX_FACTOR = 0.125f;
    private static final double REWARD_DISCOUNT_FACTOR = 1.0f;
    public TreeNode root = null;
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
        int count = 0;
        while (timer.getRemainingTime() > 0) {
            ++count;
            TreeNode nodeSelected = selectAndExpand();
            double reward = simulate(nodeSelected);
            backpropagate(nodeSelected, reward);
        }

        System.out.println("Depth: " + depth);
        TreeNode bestNode = bestChild(root, false);
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
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION) && node.children.size() == 0) {
                node = node.expandOne();
            } else if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                node = node.expandAll();
            }
            depth = Math.max(depth, node.depth) - root.depth;
        }
        return node;
    }

    private TreeNode selectAndExpand() {
        TreeNode current = root;
        while (!current.isLeaf()) {
            if (current.snapshotVersion != root.snapshotVersion) {
                current.updateSnapshot();
            }
            TreeNode next = bestChild(current, true);
            int n = current.visitCount;
            int expands = current.children.size();
//            System.out.println("Expands: " + expands);
            if (n > 0 && enhancements.contains(Enhancement.PARTIAL_EXPANSION) && current.children.size() < Utils.availableActions.length) {
                double unexploredConf = 0.5 + EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / (1 + expands));
                if (expands == 0 || unexploredConf > current.maxConfidence) {
                    return current.expandOne();
                }
            }

            current = next;
        }

        return expandIfNeeded(current);
    }

    private TreeNode bestChild(TreeNode current, boolean explore) {
        current.maxConfidence = Double.NEGATIVE_INFINITY;
        TreeNode best = null;
        for (TreeNode child : current.children) {
            double conf = calcConfidence(child, explore);
            if (conf > current.maxConfidence) {
                current.maxConfidence = conf;
                best = child;
            }
        }

        return best;
    }

    private double calcConfidence(TreeNode node, boolean explore) {
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
        double exploration = 0.0f;
        if (explore) {
            exploration = EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / nj);
        }
        return exploitation + exploration;
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
        if (simulationNode.isGameOver()) {
            if (simulationNode.sceneSnapshot.getGameStatus() == GameStatus.WIN) {
                return 1.0f;
            } else {
                return 0.0f;
            }
        } else {
            return calcReward(simulationNode.sceneSnapshot.getMarioFloatPos()[0],
                    selectedNode.parent.sceneSnapshot.getMarioFloatPos()[0]);
        }
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
