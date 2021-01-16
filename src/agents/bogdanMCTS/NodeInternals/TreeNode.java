package agents.bogdanMCTS.NodeInternals;

import agents.bogdanMCTS.MCTree;
import agents.bogdanMCTS.RNG;
import agents.bogdanMCTS.Utils;
import engine.core.MarioForwardModel;
import engine.helper.GameStatus;

import java.util.*;

public class TreeNode {
    TreeNodeData data;
    TreeNode parent;
    ArrayList<TreeNode> children;

    public TreeNode(int actionId, TreeNode parent) {
        this.data = new TreeNodeData(actionId);

        this.parent = parent;
        if (parent != null) {
            this.data.depth = parent.data.depth + 1;
        }

        this.children = new ArrayList<>();
    }

    public TreeNode(int actionId, TreeNode parent, MarioForwardModel sceneSnapshot) {
        this.data = new TreeNodeData(actionId, sceneSnapshot);

        this.parent = parent;
        if (parent != null) {
            this.data.depth = parent.data.depth + 1;
        }

        this.children = new ArrayList<>();
    }

    public MarioForwardModel getSceneSnapshot() {
        return data.sceneSnapshot;
    }

    public int getSnapshotVersion() {
        return data.snapshotVersion;
    }

    public TreeNode getParent() {
        return parent;
    }

    public ArrayList<TreeNode> getChildren() {
        return children;
    }

    public int getVisitCount() {
        return data.visitCount;
    }

    public int getDepth() {
        return data.depth;
    }

    public int getActionId() {
        return data.actionId;
    }

    public int getChildrenSize() {
        return children.size();
    }

    public double getMaxConfidence() {
        return data.maxConfidence;
    }

    public TreeNode getBestChild(boolean explore) {
        double maxConfidence = Double.NEGATIVE_INFINITY;
        TreeNode best = null;
        for (TreeNode child : children) {
            double confidence = child.calcConfidence(explore);
            if (confidence > maxConfidence) {
                maxConfidence = confidence;
                best = child;
            }
        }
        setMaxConfidence(maxConfidence);

        return best;
    }

    public void setMaxConfidence(double maxConfidence) {
        data.maxConfidence = maxConfidence;
    }

    public void setSceneSnapshot(MarioForwardModel model) {
        data.sceneSnapshot = model.clone();
    }

    public TreeNode expandAll() {
        simulatePos();

        // Expand node to all possible actions
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            children.add(NodePool.allocateNode(i, this, null));
        }

        // Return random node among expands
        return children.get(RNG.nextInt(children.size()));
    }

    public TreeNode expandOne() {
        simulatePos();

        Set<Integer> ids = new HashSet<>();
        for (TreeNode treeNode : children) {
            ids.add(treeNode.data.actionId);
        }
        List<Integer> freeIds = new ArrayList<>();
        for (int i = 0; i < Utils.availableActions.length; ++i) {
            if (!ids.contains(i)) {
                freeIds.add(i);
            }
        }
        int newId = freeIds.get(RNG.nextInt(freeIds.size()));
        TreeNode child = NodePool.allocateNode(newId, this, null);
        children.add(child);
        return child;
    }

    public void simulatePos() {
        if (parent != null && data.sceneSnapshot == null) {
            data.sceneSnapshot = parent.data.sceneSnapshot.clone();

            for (int i = 0; i < MCTree.getRepetitions(); i++) {
                if (Utils.availableActions.length > data.actionId) {
                    data.sceneSnapshot.advance(Utils.availableActions[data.actionId]);
                }
            }
        }
    }

    public boolean[] getRandomMove() {
        int rand = RNG.nextInt(Utils.availableActions.length);
        return Utils.availableActions[rand];
    }

    public List<boolean[]> getAllMoves() {
        return Arrays.asList(Utils.availableActions.clone());
    }

    public double calcConfidence(boolean explore) {
        int n = parent.getChildrenSize();
        int nj = getChildrenSize();
        if (nj == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double exploitation;
        if (MCTree.getEnhancements().contains(MCTree.Enhancement.MIXMAX)) {
            double mixMax = MCTree.getMixMaxFactor();
            exploitation = mixMax * data.maxReward + (1.0f - mixMax) * data.averageReward;
        } else {
            exploitation = data.averageReward;
        }
        double exploration = 0.0f;
        if (explore) {
            exploration = MCTree.getExplorationFactor() * Math.sqrt(2 * Math.log(n) / nj);
        }
        return exploitation + exploration;
    }

    public void makeMove(boolean[] move) {
        data.sceneSnapshot.advance(move);
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
        if (this.data.sceneSnapshot == null) {
            return false;
        }
        return data.sceneSnapshot.getGameStatus() == GameStatus.LOSE;
    }

    public boolean isWin() {
        if (this.data.sceneSnapshot == null) {
            return false;
        }
        return data.sceneSnapshot.getGameStatus() == GameStatus.WIN;
    }

    public void updateReward(double reward) {
        ++data.visitCount;
        data.totalReward += reward;
        data.maxReward = Math.max(data.maxReward, reward);
        data.averageReward = data.totalReward / data.visitCount;
    }

    public void poolSnapshotFromParent() {
        data.sceneSnapshot = parent.data.sceneSnapshot.clone();
        if (data.visitCount > 0) {
            System.out.println("WARNING: VISIT COUNT > 0");
        }
        for (int i = 0; i < MCTree.getRepetitions(); i++) {
            data.sceneSnapshot.advance(Utils.availableActions[data.actionId]);
        }
    }

    public void clearSubTree() {
        for (TreeNode child : children) {
            child.clearSubTree();
        }
        NodePool.deallocateNode(this);
    }

    public void detachFromTree() {
        // Break links between a parent and the current node.
        if (parent != null) {
            parent.children.remove(this);
        }
        parent = null;
        recalculateSubTreeDepth(0);
    }

    public int getMaxSubTreeDepth() {
        if (!children.isEmpty()) {
            return children.get(0).getMaxSubTreeDepth();
        }
        return data.depth;
    }

    private void recalculateSubTreeDepth(int newDepth) {
        data.depth = newDepth;
        for (var child : children) {
            child.recalculateSubTreeDepth(newDepth + 1);
        }
    }
}
