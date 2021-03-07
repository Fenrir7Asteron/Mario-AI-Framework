package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.NodeInternals.TreeNode;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.RNG;
import org.apache.commons.math3.util.Pair;

import java.util.*;
import java.util.concurrent.*;

import static com.mycompany.app.agents.bogdanMCTS.MCTree.expand;
import static com.mycompany.app.agents.bogdanMCTS.MCTree.select;

public class WU_UCT {
    private final static int MAX_EXPANSION_WORKERS = 16;
    private final static int MAX_SIMULATION_WORKERS = 16;

    private static final ThreadPoolExecutor _expansionWorkers = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_EXPANSION_WORKERS);
    private static final ThreadPoolExecutor _simulationWorkers = (ThreadPoolExecutor) Executors.newFixedThreadPool(MAX_SIMULATION_WORKERS);
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

//                _expansionFutures[_expansionWorkersBusyCount++] = CompletableFuture.supplyAsync(
                _expansionFutures.add(CompletableFuture.supplyAsync(
                        () -> MCTree.expand(nodeToExpand),
                        _expansionWorkers
                ));
            }

            if (_expansionFutures.size() == MAX_EXPANSION_WORKERS) {
                var expansionFuture = _expansionFutures.removeFirst();
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
                _simulationFutures.size() < MAX_SIMULATION_WORKERS
                        && !_unscheduledSimulationTasks.isEmpty()
        ) {
            var task = _unscheduledSimulationTasks.remove(
                    RNG.nextInt(_unscheduledSimulationTasks.size())
            );

            var nodeToSimulate = _simulationTaskRecorder.get(task);

            incompleteUpdate(nodeToSimulate);

//            _simulationFutures.add(CompletableFuture.supplyAsync(
            _simulationFutures.add(CompletableFuture.supplyAsync(
                    () -> new Pair(MCTree.simulate(nodeToSimulate), task),
                    _simulationWorkers
            ));
        }

        if (_simulationFutures.size() == MAX_SIMULATION_WORKERS) {
            var simulationFuture = _simulationFutures.removeFirst();

            try {
                Pair<Double, Integer> simulationResult = (Pair<Double, Integer>) simulationFuture.get();
                var simulatedNode = _simulationTaskRecorder.get(simulationResult.getSecond());
                MCTree.backpropagate(simulatedNode, simulationResult.getFirst());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        ++_searchId;
    }

    private static void incompleteUpdate(TreeNode node) {
        var currentNode = node;
        while (currentNode != null) {
            currentNode.incrementVisitCount();
            currentNode = currentNode.getParent();
        }
    }

    public static void clear() {
        while (!_expansionFutures.isEmpty()) {
            var future = _expansionFutures.removeFirst();
            future.cancel(true);
        }

        while (!_simulationFutures.isEmpty()) {
            var future = _simulationFutures.removeFirst();
            future.cancel(true);
        }

        try {
            _expansionWorkers.awaitTermination(0, TimeUnit.MILLISECONDS);
            _simulationWorkers.awaitTermination(0, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        _expansionFutures.clear();
        _simulationFutures.clear();
        _unscheduledExpansionTasks.clear();
        _unscheduledSimulationTasks.clear();
    }
}
