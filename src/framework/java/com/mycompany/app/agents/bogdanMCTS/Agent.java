package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.utils.FileWriter;
import com.mycompany.app.utils.Score;

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
        if (tree == null) {
            tree = new MCTree(model);
        } else {
            TreeNode root = tree.getRoot();
            if (root != null) {
                root.clearSubTree();
            }

            tree.updateModel(model);
        }
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
        return "MCTSAgentResIV";
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
    public void outputScores(int numberOfSamples, int levelRepetitions, int enhancements, Boolean loadResultsToGit) {
        String namePrefix = getAgentName()
                + numberOfSamples
                + MCTSEnhancements.enhancementsToString(enhancements);

        ArrayList<Double> averageLevelScores = Utils.averageLevelScores(levelRepetitions, resultScores);

        FileWriter.outputScoresToFile(numberOfSamples, averageLevelScores, DATA_FOLDER, namePrefix, loadResultsToGit);
    }

    @Override
    public void clearScores() {
        resultScores.clear();
        resultTimes.clear();
    }

    public void setEnhancements(int enhancementMask) {
        if (tree == null) {
            tree = new MCTree(null);
        }

        tree.setEnhancements(enhancementMask);
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
