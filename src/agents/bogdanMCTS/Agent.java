package agents.bogdanMCTS;

import agents.bogdanMCTS.MCTree.Enhancement;
import engine.core.MarioAgent;
import engine.core.MarioForwardModel;
import engine.core.MarioTimer;

import java.util.*;

/**
 * @author BogdanFedotov
 */
public class Agent implements MarioAgent, MachineLearningModel {
    public enum Hyperparameter {
        MAX_DEPTH,
        EXPLORATION_FACTOR,
        MIXMAX_MAX_FACTOR,
    }

    private MCTree tree = null;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        HashSet<Enhancement> enhancements = new HashSet<>();
        enhancements.add(Enhancement.MIXMAX);
        enhancements.add(Enhancement.PARTIAL_EXPANSION);
        enhancements.add(Enhancement.TREE_REUSE);
        enhancements.add(Enhancement.LOSS_AVOIDANCE);
        RNG.createRNG();
        tree = new MCTree(model, 1, enhancements);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        tree.updateModel(model);
        boolean[] action = tree.search(timer);
//        System.out.println(tree.depth);
        return action;
    }

    @Override
    public String getAgentName() {
        return "MyMCTSAgent";
    }

    @Override
    public void setHyperParameters(HashMap<Integer, Number> hyperParameters) {
        for (var hp : Hyperparameter.values()) {
            if (!hyperParameters.containsKey(hp.ordinal())) {
                throw new IllegalArgumentException("Wrong hyperparameters. Please, use corresponding public enum.");
            }
        }
        MCTree.EXPLORATION_FACTOR = (double) hyperParameters.get(Hyperparameter.EXPLORATION_FACTOR.ordinal());
        MCTree.MIXMAX_MAX_FACTOR = (double) hyperParameters.get(Hyperparameter.MIXMAX_MAX_FACTOR.ordinal());
        MCTree.MAX_SIMULATION_DEPTH = (int) hyperParameters.get(Hyperparameter.MAX_DEPTH.ordinal());
    }

    @Override
    public HashMap<Integer, List<Number>> getHyperParameterGrid() {
        HashMap<Integer, List<Number>> grid = new HashMap<>();
        grid.put(Hyperparameter.EXPLORATION_FACTOR.ordinal(), Arrays.asList(0.0, 0.125, 0.188, 0.25));
        grid.put(Hyperparameter.MIXMAX_MAX_FACTOR.ordinal(), Arrays.asList(0.0, 0.125, 0.25, 1));
        grid.put(Hyperparameter.MAX_DEPTH.ordinal(), Arrays.asList(2, 4, 6, 10));
        return grid;
    }
}
