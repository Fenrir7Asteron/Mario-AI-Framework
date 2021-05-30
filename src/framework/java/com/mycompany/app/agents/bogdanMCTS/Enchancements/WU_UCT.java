package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTSEnhancements;
import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.SimulationResult;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.RNG;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.*;

public class WU_UCT {
    public static int MAX_EXPANSION_WORKERS = 6;
    public static int MAX_SIMULATION_WORKERS = 16 - MAX_EXPANSION_WORKERS;

    private ExecutorService _expansionWorkers = Executors.newFixedThreadPool(MAX_EXPANSION_WORKERS);
    private ExecutorService _simulationWorkers = Executors.newFixedThreadPool(MAX_SIMULATION_WORKERS);
    private LinkedList<CompletableFuture> _expansionFutures = new LinkedList<>();
    private LinkedList<CompletableFuture> _simulationFutures = new LinkedList<>();
    private ArrayList<Integer> _unscheduledExpansionTasks = new ArrayList<>();
    private ArrayList<Integer> _unscheduledSimulationTasks = new ArrayList<>();
    private HashMap<Integer, TreeNode> _expansionTaskRecorder = new HashMap<>();
    private HashMap<Integer, TreeNode> _simulationTaskRecorder = new HashMap<>();
    private int _searchId;

    public void makeOneSearchStep(MCTree tree) {
        TreeNode root = tree.getRoot();
        TreeNode selectedNode = tree.select(root);

        if (tree.isExpandNeededForSelection(selectedNode)
                && selectedNode.getScheduledExpansions() < Utils.availableActions.length) {
            _unscheduledExpansionTasks.add(_searchId);
            _expansionTaskRecorder.put(_searchId, selectedNode);

            while (
                    _expansionFutures.size() < MAX_EXPANSION_WORKERS
                    && !_unscheduledExpansionTasks.isEmpty()
            ) {
                var task = _unscheduledExpansionTasks.remove(
                        RNG.nextInt(_unscheduledExpansionTasks.size())
                );

                var nodeToExpand = _expansionTaskRecorder.get(task);

                if (MCTSEnhancements.MaskContainsEnhancement(tree.getEnhancements(),
                        MCTSEnhancements.Enhancement.PARTIAL_EXPANSION)) {
                    int prevScheduledExpansionCount = nodeToExpand.getScheduledExpansions();
                    nodeToExpand.setScheduledExpansions(prevScheduledExpansionCount + 1);
                } else {
                    nodeToExpand.setScheduledExpansions(Utils.availableActions.length);
                }

                try {
                    _expansionFutures.add(CompletableFuture.supplyAsync(
                            () -> tree.expand(nodeToExpand),
                            _expansionWorkers
                    ));
                } catch (RejectedExecutionException ignored) {

                }
            }

            if (_expansionFutures.size() == MAX_EXPANSION_WORKERS) {
                waitExpansionWorker();
            }
        } else {
            _unscheduledSimulationTasks.add(_searchId);
            _simulationTaskRecorder.put(_searchId, selectedNode);
        }

        while (
                _simulationFutures.size() < MAX_SIMULATION_WORKERS
                        && !_unscheduledSimulationTasks.isEmpty()
        ) {
            var task = _unscheduledSimulationTasks.remove(
                    RNG.nextInt(_unscheduledSimulationTasks.size())
            );

            var nodeToSimulate = _simulationTaskRecorder.get(task);

            incompleteUpdate(nodeToSimulate);

            try {
                _simulationFutures.add(CompletableFuture.supplyAsync(
                        () -> new Pair<>(nodeToSimulate.getTree().simulate(nodeToSimulate), task),
                        _simulationWorkers
                ));
            } catch (RejectedExecutionException ignored) {

            }
        }

        if (_simulationFutures.size() == MAX_SIMULATION_WORKERS) {
            waitSimulationWorker(tree);
        }

        ++_searchId;
    }

    private void incompleteUpdate(TreeNode node) {
        var currentNode = node;
        while (currentNode != null) {
            currentNode.incrementIncompleteVisitCount();
            currentNode = currentNode.getParent();
        }
    }

    private void waitExpansionWorker() {
        var expansionFuture = _expansionFutures.removeFirst();
        try {
            var nodeToSimulate = (TreeNode) expansionFuture.get();
            _unscheduledSimulationTasks.add(_searchId);
            _simulationTaskRecorder.put(_searchId, nodeToSimulate);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private void waitSimulationWorker(MCTree tree) {
        var simulationFuture = _simulationFutures.removeFirst();

        try {
            Pair<SimulationResult, Integer> simulationWorkerResult = (Pair<SimulationResult, Integer>) simulationFuture.get();
            SimulationResult simulationResult = simulationWorkerResult.getFirst();
            int task = simulationWorkerResult.getSecond();
            var simulatedNode = _simulationTaskRecorder.get(task);

            if (MCTSEnhancements.MaskContainsEnhancement(tree.getEnhancements(),
                    MCTSEnhancements.Enhancement.N_GRAM_SELECTION)) {
                simulatedNode.getTree().getNGramSelection().updateRewards(
                        simulationResult.moveHistory,
                        simulationResult.reward
                );
            }
            tree.backpropagate(simulatedNode, simulationResult.reward);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public void clear(MCTree tree) {
        while (_expansionFutures.size() > 0) {
            waitExpansionWorker();
        }

        while (_simulationFutures.size() > 0) {
            waitSimulationWorker(tree);
        }

//        try {
//            _expansionWorkers.awaitTermination(1, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        try {
//            _simulationWorkers.awaitTermination(1, TimeUnit.SECONDS);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        _expansionFutures.clear();
        _simulationFutures.clear();
        _unscheduledExpansionTasks.clear();
        _unscheduledSimulationTasks.clear();
        _expansionTaskRecorder.clear();
        _simulationTaskRecorder.clear();
        _searchId = 0;
    }
}
