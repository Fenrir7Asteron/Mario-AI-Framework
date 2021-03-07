package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.MixMax;
import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.utils.RNG;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.helper.GameStatus;

import java.util.*;

public class TreeNode implements Cloneable {
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

    public double getAverageReward() {
        return data.averageReward;
    }

    public TreeNode getBestChild(boolean explore) {
        TreeNode best = children.get(0);
        double maxConfidence = best.calcConfidence(explore);

        for (int i = 1; i < children.size(); ++i) {
            var child = children.get(i);

            double confidence = child.calcConfidence(explore);
            if (confidence > maxConfidence) {
                maxConfidence = confidence;
                best = child;
            }
        }

        setMaxConfidence(maxConfidence);

        return best;
    }

    public int getScheduledExpansions() {
        return data.scheduledExpansions;
    }

    public void setMaxConfidence(double maxConfidence) {
        data.maxConfidence = maxConfidence;
    }

    public void setSceneSnapshot(MarioForwardModel model) {
        data.sceneSnapshot = model.clone();
    }

    public void setScheduledExpansions(int value) {
        data.scheduledExpansions = value;
    }

    public void incrementVisitCount() {
        data.visitCount++;
    }

    public synchronized TreeNode expandAll() {
        // Expand node to all possible actions
        var freeIds = getFreeIds();
        for (var newId : freeIds) {
            TreeNode child = NodeBuilder.allocateNode(newId, this, null);
            child.simulatePos();
            children.add(child);
        }

        // Return random node among expands
        return children.get(RNG.nextInt(children.size()));
    }

    public synchronized TreeNode expandOne() {
        var freeIds = getFreeIds();
        if (freeIds.size() > 0) {
            int newId = freeIds.get(RNG.nextInt(freeIds.size()));
            TreeNode child = NodeBuilder.allocateNode(newId, this, null);
            child.simulatePos();
            children.add(child);
            return child;
        } else {

            // Return random node among expands
            return children.get(RNG.nextInt(children.size()));
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        TreeNode cloned = (TreeNode) super.clone();
        if (cloned.data != null) {
            cloned.data = (TreeNodeData) cloned.data.clone();
        }
        if (cloned.parent != null) {
            cloned.parent = (TreeNode) cloned.parent.clone();
        }
        if (cloned.children != null) {
            cloned.children = (ArrayList<TreeNode>) cloned.children.clone();
        }
        return cloned;
    }

    private List<Integer> getFreeIds() {
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

        return freeIds;
    }

    public void simulatePos() {
        if (parent != null && data.sceneSnapshot == null) {
            data.sceneSnapshot = parent.getSceneSnapshot().clone();

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
        if (isPruned()) {
            return Double.NEGATIVE_INFINITY;
        }

        int n = parent.getChildrenSize();
        int nj = getChildrenSize();

        double exploitation;
        if (MCTree.getEnhancements().contains(MCTree.Enhancement.MIXMAX)) {
            exploitation = MixMax.getExploitation(data.averageReward, data.maxReward);
        } else {
            exploitation = data.averageReward;
        }
        double exploration = 0.0f;
        if (explore) {
            if (nj == 0) {
                return Double.POSITIVE_INFINITY;
            }

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

    public boolean isPruned() {
        return data.isPruned;
    }

    public void prune() {
        data.isPruned = true;
        data.visitCount = 0;
        data.maxReward = MCTree.MIN_REWARD;
        data.totalReward = 0.0f;
        updateReward(MCTree.MIN_REWARD);
    }

    public void updateReward(double reward) {
        if (!MCTree.getEnhancements().contains(MCTree.Enhancement.WU_UCT)) {
            incrementVisitCount();
        }

        data.totalReward += reward;
        data.maxReward = Math.max(data.maxReward, reward);
        data.averageReward = data.totalReward / data.visitCount;
    }

    public void poolSnapshotFromParent() {
        data.sceneSnapshot = parent.data.sceneSnapshot.clone();
//        if (data.visitCount > 0) {
//            System.out.println("WARNING: VISIT COUNT > 0");
//        }
        for (int i = 0; i < MCTree.getRepetitions(); i++) {
            data.sceneSnapshot.advance(Utils.availableActions[data.actionId]);
        }
    }

    public void clearSubTree() {
        for (int i = 0; i < children.size(); ++i) {
            var child = children.get(i);
            child.clearSubTree();
        }
        NodeBuilder.deallocateNode(this);
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
        for (int i = 0; i < children.size(); ++i) {
            var child = children.get(i);
            child.recalculateSubTreeDepth(newDepth + 1);
        }
    }
}
