package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.engine.core.MarioForwardModel;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class NodeBuilder {
//    private static final int MAX_POOL_SIZE = 2000;
//
//    private static final LinkedBlockingQueue<TreeNode> nodePool = new LinkedBlockingQueue<>();

    public static TreeNode allocateNode(int actionId, TreeNode parent,
                                        MarioForwardModel sceneSnapshot) {
//        if (nodePool.size() > 0) {
//            var node = nodePool.remove();
//            node.data.actionId = actionId;
//            node.data.sceneSnapshot = sceneSnapshot;
//            node.parent = parent;
//
//            return node;
//        }
        return new TreeNode(actionId, parent, sceneSnapshot);
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
