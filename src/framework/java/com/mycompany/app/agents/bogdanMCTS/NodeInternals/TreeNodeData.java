package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.engine.core.MarioForwardModel;

class TreeNodeData implements Cloneable {
    MarioForwardModel sceneSnapshot = null;

    double maxReward;
    double maxConfidence;
    double totalReward;
    double averageReward;
    int visitCount;
    int depth;
    int actionId;
    int scheduledExpansions;
    boolean isPruned;

    TreeNodeData(int actionId) {
        this.actionId = actionId;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.visitCount = 0;
        this.depth = 0;
        this.isPruned = false;
        this.scheduledExpansions = 0;
    }

    TreeNodeData(int actionId, MarioForwardModel sceneSnapshot) {
        this.actionId = actionId;
        this.sceneSnapshot = sceneSnapshot;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.visitCount = 0;
        this.depth = 0;
        this.isPruned = false;
        this.scheduledExpansions = 0;
    }

    public void clear() {
        this.actionId = -1;
        this.sceneSnapshot = null;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.visitCount = 0;
        this.depth = 0;
        this.isPruned = false;
        this.scheduledExpansions = 0;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        TreeNodeData cloned = (TreeNodeData) super.clone();
        if (cloned.sceneSnapshot != null) {
            cloned.sceneSnapshot = cloned.sceneSnapshot.clone();
        }
        return cloned;
    }
}
