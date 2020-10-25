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

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        enhancements = new HashSet<Enhancement>() {};
        enhancements.add(Enhancement.MIXMAX);
        enhancements.add(Enhancement.PARTIAL_EXPANSION);
        random = new Random();
        tree = new MCTree(model, 1, enhancements, random);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
//        tree = new MCTree(model, 1, enhancements, random);
        tree.updateModel(model);
        boolean[] action = tree.search(timer);
//        for (int i = 0; i < action.length; ++i) {
//            System.out.print(action[i] + " ");
//        }
//        System.out.println();
        return action;
    }

    @Override
    public String getAgentName() {
        return "MyMCTSAgent";
    }
}
