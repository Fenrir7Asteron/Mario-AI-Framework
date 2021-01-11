package agents.bogdanMCTS;

import engine.core.MarioForwardModel;

import java.util.Random;
import java.util.Stack;

public class NodePool {
    private static Stack<TreeNode> nodePool;

    public static void createPool() {
        nodePool = new Stack<>();
    }

    public static TreeNode allocateNode(int actionId, int repetitions, Random random, TreeNode parent,
                                        MarioForwardModel sceneSnapshot) {
        if (!nodePool.isEmpty()) {
            TreeNode node = nodePool.pop();
            node.actionId = actionId;
            node.repetitions = repetitions;
            node.random = random;
            node.parent = parent;
            node.sceneSnapshot = sceneSnapshot;
            return node;
        }
        return new TreeNode(actionId, repetitions, random, parent, sceneSnapshot);
    }

    public static void deallocateNode(TreeNode node) {
        node.depth = 0;
        node.sceneSnapshot = null;
        node.snapshotVersion = 0;
        node.visitCount = 0;
        node.maxConfidence = 0;
        node.maxReward = 0;
        node.totalReward = 0;
        node.actionId = -1;
        node.children.clear();
        node.parent = null;
        nodePool.push(node);
    }

    public static TreeNode cloneNode(TreeNode sourceNode) {
        return allocateNode(sourceNode.actionId, sourceNode.repetitions, sourceNode.random,
                sourceNode.parent, sourceNode.sceneSnapshot.clone());
    }
}
