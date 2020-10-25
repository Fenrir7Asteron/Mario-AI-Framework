package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import engine.helper.MarioActions;

import java.util.*;

public class TreeNode {
    public TreeNode parent = null;
    public ArrayList<TreeNode> children;
    public MarioForwardModel sceneSnapshot = null;
    public Random random = null;
    public int distanceFromOrigin = 0;
    public double maxReward = 0;
    public double totalReward = 0;
    public double averageReward = 0;
    public int visitCount = 0;
    public int maxDepth = 5;
    public int id;
    boolean[] action;
    int repetitions = 1;

    public TreeNode(int id, boolean[] action, int repetitions, int maxDepth, Random random, TreeNode parent) {
        this.id = id;
        this.action = action;
        this.repetitions = repetitions;
        this.maxDepth = maxDepth;
        this.random = random;
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public void initializeRoot(MarioForwardModel model) {
        if (this.parent == null) {
            this.sceneSnapshot = model.clone();
            action = new boolean[MarioActions.numberOfActions()];
        }
    }

    public TreeNode expandAll() {
        // Expand node to all possible actions
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            children.add(new TreeNode(i, Utils.getAction(i), repetitions, maxDepth, random, this));
        }

        // Return random node among expands
        return children.get(random.nextInt(children.size()));
    }

    public TreeNode expandOne() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < children.size(); ++i) {
            ids.add(children.get(i).id);
        }
        List<Integer> freeIds = new ArrayList<>();
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            if (!ids.contains(i)) {
                freeIds.add(i);
            }
        }
        int newId = freeIds.get(random.nextInt(freeIds.size()));
        TreeNode child = new TreeNode(newId, Utils.getAction(newId), repetitions, maxDepth, random, this);
        children.add(child);
        return child;
    }

    public void simulatePos() {
        if (parent != null) {
            this.sceneSnapshot = parent.sceneSnapshot.clone();
        }
        for (int i = 0; i < repetitions; i++) {
            this.sceneSnapshot.advance(action);
        }
    }

    public void randomMove() {
        this.sceneSnapshot.advance(Utils.getAction(random.nextInt(Utils.availableActions.length)));
//        this.sceneSnapshot.advance(Utils.getAction(1));
    }

    public boolean isRoot() {
        return parent == null;
    }
    public boolean isLeaf() {
        return children.isEmpty();
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

    void updateModel(MarioForwardModel model) {
        this.sceneSnapshot = model;
        for (TreeNode child : children) {
            child.updateModel(model);
        }
    }
}
