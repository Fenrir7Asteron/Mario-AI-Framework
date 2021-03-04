package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.utils.RNG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.mycompany.app.agents.bogdanMCTS.MCTree.select;

public class WU_UCT {
    private final static int MAX_EXPANSION_WORKERS = 12;
    private final static int MAX_SIMULATION_WORKERS = 12;

    private static final ExecutorService _expansionWorkers = Executors.newFixedThreadPool(MAX_EXPANSION_WORKERS);
    private static final ExecutorService _simulationWorkers = Executors.newFixedThreadPool(MAX_SIMULATION_WORKERS);
    private static CompletableFuture[] _expansionFutures = new CompletableFuture[MAX_EXPANSION_WORKERS];
    private static CompletableFuture[] _simulationFutures = new CompletableFuture[MAX_SIMULATION_WORKERS];
    private static ArrayList<Integer> _unscheduledExpansionTasks = new ArrayList<>();
    private static ArrayList<Integer> _unscheduledSimulationTasks = new ArrayList<>();
    private static HashMap<Integer, TreeNode> _expansionTaskRecorder = new HashMap<>();
    private static HashMap<Integer, TreeNode> _simulationTaskRecorder = new HashMap<>();
    private static int _searchId;
    private static int _expansionWorkersBusyCount;
    private static int _simulationWorkersBusyCount;

    public static void makeOneSearchStep(TreeNode root) {
        TreeNode selectedNode = select(root);

        if (MCTree.needExpand()) {
            _unscheduledExpansionTasks.add(_searchId);
            _expansionTaskRecorder.put(_searchId, selectedNode);

            while (
                    _expansionWorkersBusyCount < MAX_EXPANSION_WORKERS
                    && !_unscheduledExpansionTasks.isEmpty()
            ) {
                _expansionFutures[_expansionWorkersBusyCount++] = CompletableFuture.supplyAsync(
                        () ->
                        {
                            var task = _unscheduledExpansionTasks.get(
                                    RNG.nextInt(_unscheduledExpansionTasks.size())
                            );

                            var nodeToExpand = _expansionTaskRecorder.get(task);
                            return MCTree.expand(nodeToExpand);
                        },
                        _expansionWorkers
                );
            }

            if (_expansionWorkersBusyCount == MAX_EXPANSION_WORKERS) {
                var expansionFuture = CompletableFuture.anyOf(_expansionFutures);
                try {
                    var nodeToSimulate = (TreeNode) expansionFuture.get();
                    _unscheduledSimulationTasks.add(_searchId);
                    _simulationTaskRecorder.put(_searchId, nodeToSimulate);
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        } else {
            _unscheduledSimulationTasks.add(_searchId);
            _simulationTaskRecorder.put(_searchId, selectedNode);
        }

        while (
                _simulationWorkersBusyCount < MAX_SIMULATION_WORKERS
                        && !_unscheduledSimulationTasks.isEmpty()
        ) {
            _simulationFutures[_simulationWorkersBusyCount++] = CompletableFuture.supplyAsync(
                    () ->
                    {
                        var task = _unscheduledSimulationTasks.get(
                                RNG.nextInt(_unscheduledSimulationTasks.size())
                        );

                        var nodeToSimulate = _simulationTaskRecorder.get(task);
                        return MCTree.simulate(nodeToSimulate);
                    },
                    _simulationWorkers
            );
        }

        if (_simulationWorkersBusyCount == MAX_SIMULATION_WORKERS) {
            var simulationFuture = CompletableFuture.anyOf(_simulationFutures);
            try {
                var reward = (Double) simulationFuture.get();
                MCTree.backpropagate(selectedNode, reward);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        ++_searchId;
    }
}
