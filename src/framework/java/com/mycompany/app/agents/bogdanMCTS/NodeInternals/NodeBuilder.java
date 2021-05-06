package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.engine.core.MarioForwardModel;

public class NodeBuilder {
//    private static final int MAX_POOL_SIZE = 2000;
//
//    private static final LinkedBlockingQueue<TreeNode> nodePool = new LinkedBlockingQueue<>();

    public static TreeNode allocateNode(int actionId,
                                        TreeNode parent,
                                        MCTree tree,
                                        MarioForwardModel sceneSnapshot, int repetitions) {
//        if (nodePool.size() > 0) {
//            var node = nodePool.remove();
//            node.data.actionId = actionId;
//            node.data.sceneSnapshot = sceneSnapshot;
//            node.parent = parent;
//
//            return node;
//        }
        return new TreeNode(actionId, parent, sceneSnapshot, tree, repetitions);
    }

    public static  void deallocateNode(TreeNode node) {
        var treeNodeData = node.data;
        treeNodeData.clear();
        node.children.clear();
        node.parent = null;

//        if (nodePool.size() < MAX_POOL_SIZE) {
//            nodePool.add(node);
//        }
    }
}
