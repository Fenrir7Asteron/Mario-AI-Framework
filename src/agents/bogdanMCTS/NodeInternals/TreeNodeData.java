package agents.bogdanMCTS.NodeInternals;

import engine.core.MarioForwardModel;

import java.util.ArrayList;
import java.util.Random;

class TreeNodeData {
    MarioForwardModel sceneSnapshot = null;

    double maxReward;
    double maxConfidence;
    double totalReward;
    double averageReward;
    int snapshotVersion;
    int visitCount;
    int depth;
    int actionId;

    TreeNodeData(int actionId) {
        this.actionId = actionId;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.snapshotVersion = 0;
        this.visitCount = 0;
        this.depth = 0;
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
    }
}
