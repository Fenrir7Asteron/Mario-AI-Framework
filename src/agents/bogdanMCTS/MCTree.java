package agents.bogdanMCTS;

import agents.bogdanMCTS.NodeInternals.NodePool;
import agents.bogdanMCTS.NodeInternals.TreeNode;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

import java.util.*;

public class MCTree {
    static int MAX_DEPTH; // = 6;
    static double EXPLORATION_FACTOR; // = 0.188f;
    static double MIXMAX_MAX_FACTOR; // = 0.125f;
    static int repetitions = 1;
    static Set<Enhancement> enhancements;

    private TreeNode root = null;
    private int depth;

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements) {
        MCTree.repetitions = repetitions;
        MCTree.enhancements = enhancements;
        NodePool.createPool();
        initializeRoot(model);
        depth = 0;
    }

    public static double getExplorationFactor() {
        return EXPLORATION_FACTOR;
    }

    public static double getMixMaxFactor() {
        return MIXMAX_MAX_FACTOR;
    }

    public static int getMaxDepth() {
        return MAX_DEPTH;
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
        while (timer.getRemainingTime() > 0) {
            ++count;
            TreeNode nodeSelected = selectAndExpand();
            double reward = simulate(nodeSelected);
            backpropagate(nodeSelected, reward);
        }
        TreeNode bestNode = root.getBestChild(false);
        int bestActionId = bestNode.getActionId();
        clearTree();
        return Utils.availableActions[bestActionId];
    }

    private void clearTree() {
        root.free();
    }

    void updateModel(MarioForwardModel model) {
//        root.sceneSnapshot = model;
//        root.snapshotVersion++;
        initializeRoot(model);
    }

    private TreeNode expandIfNeeded(TreeNode node) {
        if (node.getVisitCount() > 0 && node.getChildrenSize() < Utils.availableActions.length) {
            if (enhancements.contains(Enhancement.PARTIAL_EXPANSION) && node.getChildrenSize() == 0) {
                node = node.expandOne();
            } else if (!enhancements.contains(Enhancement.PARTIAL_EXPANSION)) {
                node = node.expandAll();
            }
            depth = Math.max(depth, node.getDepth()) - root.getDepth();
        }
        return node;
    }

    private TreeNode selectAndExpand() {
        TreeNode current = root;
        while (!current.isLeaf()) {
            if (current.getSnapshotVersion() != root.getSnapshotVersion()) {
                current.updateSnapshot();
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

        TreeNode simulationNode = NodePool.cloneNode(sourceNode);

        int step = 0;
        ArrayList<boolean[]> moveHistory = new ArrayList<>();

        while (step < MAX_DEPTH) {
            ++step;

            var nextMove = simulationNode.getRandomMove();
            simulationNode.makeMove(nextMove);

            if (simulationNode.isLost()) {
                if (enhancements.contains(Enhancement.LOSS_AVOIDANCE)) {
                    // Create another simulation node and advance it until one move before the loss.
                    TreeNode lossAvoidingSimulationNode = NodePool.cloneNode(sourceNode);
                    lossAvoidingSimulationNode.makeMoves(moveHistory);

                    List<boolean[]> availableMoves = lossAvoidingSimulationNode.getAllMoves();
                    double maxReward = 0.0f;

                    // Try all available moves and return the best result.
                    // During the Loss Avoidance max simulation depth is ignored for the optimization purposes.
                    for (var moveVariant : availableMoves) {
                        TreeNode nodeVariant = NodePool.cloneNode(lossAvoidingSimulationNode);
                        nodeVariant.makeMove(moveVariant);
                        maxReward = Math.max(maxReward, calcReward(sourceNode.getSceneSnapshot(),
                                nodeVariant.getSceneSnapshot()));
                    }

                    return maxReward;
                } else {
                    return 0.0;
                }
            } else if (simulationNode.getSceneSnapshot().getGameStatus() == GameStatus.WIN) {
                return 1.0;
            }

            if (enhancements.contains(Enhancement.LOSS_AVOIDANCE)) {
                moveHistory.add(nextMove);
            }
        }

        // Return reward at the end of a simulation.
        return calcReward(sourceNode.getSceneSnapshot(), simulationNode.getSceneSnapshot());
    }

    private double calcReward(MarioForwardModel startSnapshot, MarioForwardModel endSnapshot) {
        double startX = startSnapshot.getMarioFloatPos()[0];
        double endX = endSnapshot.getMarioFloatPos()[0];
        int damage = Math.max(0, startSnapshot.getMarioMode() - endSnapshot.getMarioMode()) +
                Math.max(0, startSnapshot.getNumLives() - endSnapshot.getNumLives());

        double reward = 0.5 + 0.5 * (endX - startX) / (11.0 * (1 + MAX_DEPTH)) - 0.5 * damage;
        if (reward < 0) {
            System.out.println("Warning: reward is less than zero, reward = " + reward);
        }
        return reward;
    }

    private void backpropagate(TreeNode currentNode, double reward) {
        while (currentNode != null) {
            currentNode.updateReward(reward);
            currentNode = currentNode.getParent();
        }
    }

    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
        LOSS_AVOIDANCE,
    }
}
