package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.agents.bogdanMCTS.Enchancements.ProcrastinationPunisher;
import com.mycompany.app.engine.core.MarioForwardModel;

import java.util.LinkedList;

class TreeNodeData implements Cloneable {
    MarioForwardModel sceneSnapshot = null;

    double maxReward;
    double maxConfidence;
    double totalReward;
    double averageReward;
    double sumOfRewardsSquared;
    float visitCount;
    float visitCountIncomplete;
    int depth;
    int actionId;
    int scheduledExpansions;
    boolean isPruned;
    int repetitions;
    boolean dangerousPlace;

    ProcrastinationPunisher procrastinationPunisher;

    TreeNodeData(int actionId) {
        new TreeNodeData(actionId, null);
    }

    TreeNodeData(int actionId, MarioForwardModel sceneSnapshot) {
        this.actionId = actionId;
        this.sceneSnapshot = sceneSnapshot;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.visitCount = 0;
        this.visitCountIncomplete = 0;
        this.depth = 0;
        this.isPruned = false;
        this.scheduledExpansions = 0;
        this.repetitions = 1;
        this.procrastinationPunisher = new ProcrastinationPunisher();
    }

    public void clear() {
        this.actionId = -1;
        this.sceneSnapshot = null;
        this.maxReward = 0;
        this.maxConfidence = 0;
        this.totalReward = 0;
        this.averageReward = 0;
        this.visitCount = 0;
        this.visitCountIncomplete = 0;
        this.depth = 0;
        this.isPruned = false;
        this.scheduledExpansions = 0;
        this.procrastinationPunisher.clear();
    }
}
