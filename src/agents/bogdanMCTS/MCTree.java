package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

public class MCTree {
    private static final int MAX_DEPTH = 6;
    private static final double EXPLORATION_FACTOR = 0.2f;
    public TreeNode root = null;
    public int repetitions = 1;

    public MCTree(MarioForwardModel model, int repetitions) {
        this.repetitions = repetitions;
        root = new TreeNode(null, repetitions, MAX_DEPTH, null);
        root.initializeRoot(model);
        root.expandNode();
    }

    public boolean[] search(MarioTimer timer) {
        while (timer.getRemainingTime() > 0) {
            TreeNode nodeSelected = selectNode();
            if (nodeSelected.visitCount > 0) {
                nodeSelected = nodeSelected.expandNode();
            }
            double reward = simulate(nodeSelected);
            backpropagate(nodeSelected, reward);
        }

//        System.out.println(root.visitCount);
        TreeNode bestNode = bestChild(root);
        root = bestNode;
        root.parent = null;
//        System.out.println(root.maxReward + " " + root.averageReward + " " + root.visitCount);
        return bestNode.action;
    }

    private TreeNode selectNode() {
        TreeNode current = root;
        while (!current.isLeaf()) {
            current = bestChild(current);
        }


        return current;
    }

    private TreeNode bestChild(TreeNode current) {
        double maxConfidence = -1.0f;
        TreeNode best = null;
        for (TreeNode child : current.children) {
            int n = current.visitCount;
            int nchild = child.visitCount;
            double conf = Double.POSITIVE_INFINITY;
            if (n > 0) {
                conf = child.averageReward + EXPLORATION_FACTOR * Math.sqrt(2 * Math.log(n) / nchild);
            }
//            System.out.println(conf + " " + child.totalReward + " " + nchild + " " + EXPLORATION_FACTOR + " " + n);
            if (conf > maxConfidence) {
                maxConfidence = conf;
                best = child;
            }
        }

        return best;
    }

    private double simulate(TreeNode selectedNode) {
        selectedNode.simulatePos();
        TreeNode simulationNode = new TreeNode(null, 1, MAX_DEPTH, null);
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
