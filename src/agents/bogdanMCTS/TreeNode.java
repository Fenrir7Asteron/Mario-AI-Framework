package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.ArrayList;
import java.util.concurrent.ThreadLocalRandom;

public class TreeNode {
    public TreeNode parent = null;
    public ArrayList<TreeNode> children;
    public MarioForwardModel sceneSnapshot = null;
    public int distanceFromOrigin = 0;
    public double maxReward = 0;
    public double totalReward = 0;
    public double averageReward = 0;
    public int visitCount = 0;
    public int maxDepth = 5;
    boolean[] action;
    int repetitions = 1;

    public TreeNode(boolean[] action, int repetitions, int maxDepth, TreeNode parent) {
        this.action = action;
        this.repetitions = repetitions;
        this.maxDepth = maxDepth;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public void initializeRoot(MarioForwardModel model) {
        if (this.parent == null) {
            this.sceneSnapshot = model.clone();
            action = new boolean[MarioActions.numberOfActions()];
        }
    }

    public TreeNode expandNode() {
        // Expand node to all possible actions
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            children.add(new TreeNode(Utils.getAction(i), repetitions, maxDepth, this));
        }

        // Return random node among expands
        return children.get(ThreadLocalRandom.current().nextInt(0, children.size()));
    }

    public double simulatePos() {
        this.sceneSnapshot = parent.sceneSnapshot.clone();
        for (int i = 0; i < repetitions; i++) {
            this.sceneSnapshot.advance(action);
        }
        if (isGameOver()) {
            if (sceneSnapshot.getGameStatus() == GameStatus.WIN) {
                return 1.0f;
            } else {
                return -1.0f;
            }
        }
        float xj = parent.sceneSnapshot.getMarioFloatPos()[0];
        float xp = this.sceneSnapshot.getMarioFloatPos()[0];
        this.maxReward = 0.5 + 0.5 * (xj - xp) / (11 * (1 + maxDepth));
        return maxReward;
    }

    public void randomMove() {
        this.sceneSnapshot.advance(Utils.getAction(ThreadLocalRandom.current().nextInt(0, Utils.availableActions.length)));
//        this.sceneSnapshot.advance(Utils.getAction(1));
    }

    public boolean isRoot() {
        return parent == null;
    }
    public boolean isLeaf() {
        return !isRoot() && children.isEmpty();
    }
    public boolean isGameOver() {
        if (this.sceneSnapshot == null) {
            return false;
        }
        return this.sceneSnapshot.getGameStatus() != GameStatus.RUNNING;
    }

    public void updateReward(double reward) {
        ++visitCount;
        totalReward += reward;
        maxReward = Math.max(maxReward, reward);
        averageReward = totalReward / visitCount;
    }
}
