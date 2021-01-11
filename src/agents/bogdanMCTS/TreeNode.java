package agents.bogdanMCTS;

import engine.core.MarioForwardModel;
import engine.helper.GameStatus;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class TreeNode {
    public TreeNode parent = null;
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

    public TreeNode(int actionId, int repetitions, Random random, TreeNode parent) {
        this.actionId = actionId;
        this.repetitions = repetitions;
        this.random = random;
        this.parent = parent;
        if (parent != null) {
            this.depth = parent.depth + 1;
        } else {
            this.depth = 0;
            this.snapshotVersion = 0;
        }
        this.children = new ArrayList<>();
    }

    public TreeNode(int actionId, int repetitions, Random random, TreeNode parent, MarioForwardModel sceneSnapshot) {
        this.actionId = actionId;
        this.repetitions = repetitions;
        this.random = random;
        this.parent = parent;
        this.sceneSnapshot = sceneSnapshot;
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
            children.add(NodePool.allocateNode(i, repetitions, random, this, null));
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
        TreeNode child = NodePool.allocateNode(newId, repetitions, random, this, null);
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

    public boolean[] getRandomMove() {
        int rand = random.nextInt(Utils.availableActions.length);
        return Utils.availableActions[rand];
    }

    public List<boolean[]> getAllMoves() {
        return Arrays.asList(Utils.availableActions.clone());
    }

    public void makeMove(boolean[] move) {
        sceneSnapshot.advance(move);
    }

    public void makeMove(boolean[] move, int repetitions) {
        for (int i = 0; i < repetitions; ++i) {
            makeMove(move);
        }
    }

    public void makeMoves(List<boolean[]> moves) {
        for (var move : moves) {
            makeMove(move);
        }
    }

    public void makeMoves(List<boolean[]> moves, int repetitions) {
        for (var move : moves) {
            makeMove(move, repetitions);
        }
    }

    public boolean isRoot() {
        return parent == null;
    }
    public boolean isLeaf() {
        return children.isEmpty();
    }
    public boolean isLost() {
        if (this.sceneSnapshot == null) {
            return false;
        }
        return sceneSnapshot.getNumLives() == -1 || sceneSnapshot.getGameStatus() == GameStatus.LOSE;
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
        NodePool.deallocateNode(this);
    }
}
