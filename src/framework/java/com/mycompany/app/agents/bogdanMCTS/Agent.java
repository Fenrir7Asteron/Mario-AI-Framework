package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.MCTree.Enhancement;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.utils.FileWriter;
import com.mycompany.app.utils.Score;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.mycompany.app.utils.MyMath.average;

/**
 * @author BogdanFedotov
 */
public class Agent implements PaperAgent {
    public enum Hyperparameter {
        MAX_DEPTH,
        EXPLORATION_FACTOR,
        MIXMAX_MAX_FACTOR,
    }

    public static final String DATA_FOLDER = "data/";

    private MCTree tree = null;
    private ArrayList<Double> resultScores = new ArrayList<>();
    private ArrayList<Double> resultTimes = new ArrayList<>();

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        HashSet<Enhancement> enhancements = new HashSet<>();
        enhancements.add(Enhancement.MIXMAX);
//        enhancements.add(Enhancement.PARTIAL_EXPANSION);
        enhancements.add(Enhancement.TREE_REUSE);
        enhancements.add(Enhancement.LOSS_AVOIDANCE);
        enhancements.add(Enhancement.HARD_PRUNING);
        enhancements.add(Enhancement.SAFETY_PREPRUNING);
        enhancements.add(Enhancement.WU_UCT);
        enhancements.add(Enhancement.AGING);
        enhancements.add(Enhancement.N_GRAM_SELECTION);
        tree = new MCTree(model, 1, enhancements);
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        tree.updateModel(model);
        boolean[] action = tree.search(timer);
//        for (var a : action) {
//            System.out.print(a + " ");
//        }
//        System.out.println();
        return action;
    }

    @Override
    public String getAgentName() {
        return "MyMCTSAgent";
    }

    @Override
    public void addResult(Score newScore) {
        resultScores.add(newScore.score);
        resultTimes.add(newScore.time);
    }

    @Override
    public double averageScore() {
        return average(resultScores);
    }

    @Override
    public double averageTime() {
        return average(resultTimes);
    }

    @Override
    public void outputScores(int numberOfSamples) {
        String namePrefix = getAgentName() + numberOfSamples + enhancementsToString();
        FileWriter.outputScoresToFile(numberOfSamples, resultScores, DATA_FOLDER, namePrefix);
    }

    private String enhancementsToString() {
        StringBuilder shortNames = new StringBuilder();

        for (var enhancement : MCTree.enhancements) {
            switch (enhancement) {
                case MIXMAX:
                    shortNames.append("+MM");
                    break;
                case TREE_REUSE:
                    shortNames.append("+TR");
                    break;
                case HARD_PRUNING:
                    shortNames.append("+HP");
                    break;
                case LOSS_AVOIDANCE:
                    shortNames.append("+LA");
                    break;
                case PARTIAL_EXPANSION:
                    shortNames.append("+PA");
                    break;
                case SAFETY_PREPRUNING:
                    shortNames.append("+SP");
                    break;
                default:
                    break;
            }
        }

        return shortNames.toString();
    }

//    @Override
//    public void setHyperParameters(HashMap<Integer, Number> hyperParameters) {
//        for (var hp : Hyperparameter.values()) {
//            if (!hyperParameters.containsKey(hp.ordinal())) {
//                throw new IllegalArgumentException("Wrong hyperparameters. Please, use corresponding public enum.");
//            }
//        }
//        MCTree.EXPLORATION_FACTOR = (double) hyperParameters.get(Hyperparameter.EXPLORATION_FACTOR.ordinal());
//        MCTree.MIXMAX_MAX_FACTOR = (double) hyperParameters.get(Hyperparameter.MIXMAX_MAX_FACTOR.ordinal());
//        MCTree.MAX_SIMULATION_DEPTH = (int) hyperParameters.get(Hyperparameter.MAX_DEPTH.ordinal());
//    }
//
//    @Override
//    public HashMap<Integer, List<Number>> getHyperParameterGrid() {
//        HashMap<Integer, List<Number>> grid = new HashMap<>();
//        grid.put(Hyperparameter.EXPLORATION_FACTOR.ordinal(), Arrays.asList(0.0, 0.125, 0.188, 0.25));
//        grid.put(Hyperparameter.MIXMAX_MAX_FACTOR.ordinal(), Arrays.asList(0.0, 0.125, 0.25, 1));
//        grid.put(Hyperparameter.MAX_DEPTH.ordinal(), Arrays.asList(2, 4, 6, 10));
//        return grid;
//    }
}
