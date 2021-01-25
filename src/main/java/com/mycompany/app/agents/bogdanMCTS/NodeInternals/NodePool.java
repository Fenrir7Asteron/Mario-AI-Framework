package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.engine.core.MarioForwardModel;

import java.util.Stack;

public class NodePool {
    public static final int MAX_POOL_SIZE = 500;

    private static Stack<TreeNode> nodePool;

    public static void createPool() {
        nodePool = new Stack<>();
    }

    public synchronized static TreeNode allocateNode(int actionId, TreeNode parent,
                                        MarioForwardModel sceneSnapshot) {
        if (!nodePool.isEmpty()) {
            var treeNode = nodePool.pop();
            var treeNodeData = treeNode.data;
            treeNodeData.actionId = actionId;
            treeNodeData.sceneSnapshot = sceneSnapshot;
            treeNode.parent = parent;
            return treeNode;
        }
        return new TreeNode(actionId, parent, sceneSnapshot);
    }

    public static void deallocateNode(TreeNode node) {
        var treeNodeData = node.data;
        treeNodeData.depth = 0;
        treeNodeData.sceneSnapshot = null;
        treeNodeData.snapshotVersion = 0;
        treeNodeData.visitCount = 0;
        treeNodeData.maxConfidence = 0;
        treeNodeData.maxReward = 0;
        treeNodeData.totalReward = 0;
        treeNodeData.actionId = -1;
        node.children.clear();
        node.parent = null;
        if (nodePool.size() < MAX_POOL_SIZE) {
            nodePool.push(node);
        }
    }
}
