package com.mycompany.app.agents.bogdanMCTS.NodeInternals;

import com.mycompany.app.engine.core.MarioForwardModel;

public class NodeBuilder {
    public static TreeNode allocateNode(int actionId, TreeNode parent,
                                        MarioForwardModel sceneSnapshot) {
        return new TreeNode(actionId, parent, sceneSnapshot);
    }

    public static void deallocateNode(TreeNode node) {
        var treeNodeData = node.data;
        treeNodeData.clear();
        node.children.clear();
        node.parent = null;
    }
}
