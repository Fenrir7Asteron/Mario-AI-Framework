package agents.bogdanMCTS.NodeInternals;

import engine.core.MarioForwardModel;

import java.util.Random;
import java.util.Stack;

public class NodePool {
    private static Stack<TreeNode> nodePool;

    public static void createPool() {
        nodePool = new Stack<>();
    }

    public static TreeNode allocateNode(int actionId, TreeNode parent,
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
        nodePool.push(node);
    }
}
