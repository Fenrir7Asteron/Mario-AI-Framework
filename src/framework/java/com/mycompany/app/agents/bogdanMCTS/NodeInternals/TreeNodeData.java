package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.engine.core.MarioForwardModel;

class TreeNodeData implements Cloneable {
    MarioForwardModel sceneSnapshot = null;

    double maxReward;
    double maxConfidence;
    double totalReward;
    double averageReward;
    int snapshotVersion;
    int visitCount;
    int depth;
    int actionId;
    boolean isPruned;

    TreeNodeData(int actionId) {
        this.actionId = actionId;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.snapshotVersion = 0;
        this.visitCount = 0;
        this.depth = 0;
        this.isPruned = false;
    }

    TreeNodeData(int actionId, MarioForwardModel sceneSnapshot) {
        this.actionId = actionId;
        this.sceneSnapshot = sceneSnapshot;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.snapshotVersion = 0;
        this.visitCount = 0;
        this.depth = 0;
        this.isPruned = false;
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
