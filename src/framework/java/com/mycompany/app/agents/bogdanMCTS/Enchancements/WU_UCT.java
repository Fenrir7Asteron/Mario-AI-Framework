package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.RNG;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.*;

import static com.mycompany.app.agents.bogdanMCTS.MCTree.select;

public class WU_UCT {
    private final static int MAX_EXPANSION_WORKERS = 4;
    private final static int MAX_SIMULATION_WORKERS = 12;

    private static ExecutorService _expansionWorkers = Executors.newFixedThreadPool(MAX_EXPANSION_WORKERS);
    private static ExecutorService _simulationWorkers = Executors.newFixedThreadPool(MAX_SIMULATION_WORKERS);
    private static LinkedList<CompletableFuture> _expansionFutures = new LinkedList<>();
    private static LinkedList<CompletableFuture> _simulationFutures = new LinkedList<>();
    private static ArrayList<Integer> _unscheduledExpansionTasks = new ArrayList<>();
    private static ArrayList<Integer> _unscheduledSimulationTasks = new ArrayList<>();
    private static HashMap<Integer, TreeNode> _expansionTaskRecorder = new HashMap<>();
    private static HashMap<Integer, TreeNode> _simulationTaskRecorder = new HashMap<>();
    private static int _searchId;

    public static void makeOneSearchStep(TreeNode root) {
        TreeNode selectedNode = select(root);

        if (MCTree.isExpandNeededForSelection(selectedNode)
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

                if (MCTree.getEnhancements().contains(MCTree.Enhancement.PARTIAL_EXPANSION)) {
                    int prevScheduledExpansionCount = nodeToExpand.getScheduledExpansions();
                    nodeToExpand.setScheduledExpansions(prevScheduledExpansionCount + 1);
                } else {
                    nodeToExpand.setScheduledExpansions(Utils.availableActions.length);
                }

                try {
                    _expansionFutures.add(CompletableFuture.supplyAsync(
                            () -> MCTree.expand(nodeToExpand),
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
                        () -> new Pair(MCTree.simulate(nodeToSimulate), task),
                        _simulationWorkers
                ));
            } catch (RejectedExecutionException ignored) {

            }
        }

        if (_simulationFutures.size() == MAX_SIMULATION_WORKERS) {
            waitSimulationWorker();
        }

        ++_searchId;
    }

    private static void incompleteUpdate(TreeNode node) {
        var currentNode = node;
        while (currentNode != null) {
            currentNode.incrementIncompleteVisitCount();
            currentNode = currentNode.getParent();
        }
    }

    private static void waitExpansionWorker() {
        var expansionFuture = _expansionFutures.removeFirst();
        try {
            var nodeToSimulate = (TreeNode) expansionFuture.get();
            _unscheduledSimulationTasks.add(_searchId);
            _simulationTaskRecorder.put(_searchId, nodeToSimulate);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private static void waitSimulationWorker() {
        var simulationFuture = _simulationFutures.removeFirst();

        try {
            Pair<Double, Integer> simulationResult = (Pair<Double, Integer>) simulationFuture.get();
            var simulatedNode = _simulationTaskRecorder.get(simulationResult.getSecond());
            MCTree.backpropagate(simulatedNode, simulationResult.getFirst());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    public static void clear() {
        while (_expansionFutures.size() > 0) {
            waitExpansionWorker();
        }

        while (_simulationFutures.size() > 0) {
            waitSimulationWorker();
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
