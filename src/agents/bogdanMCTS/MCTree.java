package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.GameStatus;

import java.util.*;

public class MCTree {
    protected static int MAX_DEPTH; // = 6;
    protected static double EXPLORATION_FACTOR; // = 0.188f;
    protected static double MIXMAX_MAX_FACTOR ; // = 0.125f;

    public TreeNode root = null;
    private Random random = null;
    private int repetitions = 1;
    private Set<Enhancement> enhancements;
    public int depth;

    public enum Enhancement {
        MIXMAX,
        PARTIAL_EXPANSION,
        LOSS_AVOIDANCE,
    }

    MCTree(MarioForwardModel model, int repetitions, Set<Enhancement> enhancements, Random random) {
        this.repetitions = repetitions;
        this.enhancements = enhancements;
        this.random = random;
        NodePool.createPool();
        initializeRoot(model);
        depth = 0;
    }

    public void initializeRoot(MarioForwardModel model) {
        root = NodePool.allocateNode(-1, repetitions, random, null, model.clone());
        root.sceneSnapshot = model.clone();
        root.snapshotVersion = 0;
        root.depth = 0;
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
        TreeNode bestNode = bestChild(root, false);
        int bestActionId = bestNode.actionId;
        System.out.println(count);
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
                        maxReward = Math.max(maxReward, calcReward(sourceNode.sceneSnapshot, nodeVariant.sceneSnapshot));
                    }

                    return maxReward;
                } else {
                    return 0.0;
                }
            }
            else if (simulationNode.sceneSnapshot.getGameStatus() == GameStatus.WIN) {
                return 1.0;
            }

            if (enhancements.contains(Enhancement.LOSS_AVOIDANCE)) {
                moveHistory.add(nextMove);
            }
        }

        // Return reward at the end of a simulation.
        return calcReward(sourceNode.sceneSnapshot, simulationNode.sceneSnapshot);
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
            currentNode = currentNode.parent;
        }
    }
}
