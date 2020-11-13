package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;

import java.util.*;

public class TreeNode {
    public TreeNode parent = null;
    public MCTree tree = null;
    public ArrayList<TreeNode> children;
    public MarioForwardModel sceneSnapshot = null;
    public int snapshotVersion;
    public Random random;
    public double maxReward = 0;
    public double maxConfidence = 0;
    public double totalReward = 0;
    public double averageReward = 0;
    public int visitCount = 0;
    public int depth;
    int actionId;
    int repetitions = 1;

    public TreeNode(int actionId, int repetitions, Random random, TreeNode parent, MCTree tree) {
        this.actionId = actionId;
        this.repetitions = repetitions;
        this.random = random;
        this.parent = parent;
        this.tree = tree;
        if (parent != null) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
            this.snapshotVersion = 0;
        }
        this.children = new ArrayList<>();
    }

    public TreeNode expandAll() {
        // Expand node to all possible actions
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            children.add(tree.allocateNode(i, repetitions, random, this));
        }

        // Return random node among expands
        return children.get(random.nextInt(children.size()));
    }

    public TreeNode expandOne() {
        Set<Integer> ids = new HashSet<>();
        for (int i = 0; i < children.size(); ++i) {
            ids.add(children.get(i).actionId);
        }
        List<Integer> freeIds = new ArrayList<>();
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            if (!ids.contains(i)) {
                freeIds.add(i);
            }
        }
        int newId = freeIds.get(random.nextInt(freeIds.size()));
        TreeNode child = tree.allocateNode(newId, repetitions, random, this);
        children.add(child);
        return child;
    }

    public void simulatePos() {
        if (parent != null) {
            this.sceneSnapshot = parent.sceneSnapshot.clone();
            this.snapshotVersion = parent.snapshotVersion;
        }
        for (int i = 0; i < repetitions; i++) {
            this.sceneSnapshot.advance(Utils.availableActions[actionId]);
        }
    }

    public void randomMove() {
        for (int i = 0; i < repetitions; i++) {
            int rand = random.nextInt(Utils.availableActions.length);
            this.sceneSnapshot.advance(Utils.availableActions[rand]);
        }
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

    public void updateSnapshot() {
        this.sceneSnapshot = parent.sceneSnapshot.clone();
        if (this.visitCount > 0) {
            System.out.println("WARNING: VISIT COUNT > 0");
        }
        for (int i = 0; i < repetitions; i++) {
            this.sceneSnapshot.advance(Utils.availableActions[actionId]);
        }
    }

    public void free() {
        for (TreeNode child : children) {
            child.free();
        }
        tree.deallocateNode(this);
    }
}
