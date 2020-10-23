package agents.bogdanMCTS;

import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;
import engine.helper.MarioActions;

/**
 * @author BogdanFedotov
 */
public class Agent implements MarioAgent {
    private MCTree tree = null;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        tree = new MCTree(model, 1);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
//        tree = new MCTree(model, 1);
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
