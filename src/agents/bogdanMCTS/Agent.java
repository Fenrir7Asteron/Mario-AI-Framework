package agents.bogdanMCTS;

import agents.bogdanMCTS.MCTree.Enhancement;
import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;

import java.util.HashSet;
import java.util.Random;

/**
 * @author BogdanFedotov
 */
public class Agent implements MarioAgent {
    private MCTree tree = null;
    private Random random = null;
    private HashSet<Enhancement> enhancements;
    private boolean[] action;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        enhancements = new HashSet<Enhancement>() {};
        enhancements.add(Enhancement.MIXMAX);
        enhancements.add(Enhancement.PARTIAL_EXPANSION);
        random = new Random(System.currentTimeMillis());
        tree = new MCTree(model, 1, enhancements, random);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        tree.updateModel(model);
        action = tree.search(timer);
//        System.out.println(tree.depth);
        return action;
    }

    @Override
    public String getAgentName() {
        return "MyMCTSAgent";
    }
}
