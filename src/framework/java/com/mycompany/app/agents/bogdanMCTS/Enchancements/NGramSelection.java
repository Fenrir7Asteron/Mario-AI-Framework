package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.Utils;
import com.mycompany.app.utils.RNG;

import java.util.Hashtable;
import java.util.List;

public class NGramSelection {
    private static final int N = 2;
    private static final int k = 7;
    private static final double DECAY_FACTOR = 0.01f;
    private static final double EPSILON = 0.2f;

    private Hashtable<String, Result> nGramResultsMapping = new Hashtable<>();

    private class Result {
        public double totalReward;
        public double visitCount;

        public Result(double reward) {
            totalReward = reward;
            visitCount = 1;
        }
    }

    public int getMove(List<Integer> moveHistory) {
        if (RNG.nextFloat() < EPSILON) {
            // Return random move with EPSILON probability.
            return RNG.nextInt(Utils.availableActions.length);
        }

        double maxReward = MCTree.MIN_REWARD - 1;
        int bestActionId = -1;

        for (int actionCandidateId = 0; actionCandidateId < Utils.availableActions.length; ++actionCandidateId) {
            double averageReward = getAverageRewardByAction(moveHistory, actionCandidateId);
            if (averageReward > maxReward) {
                maxReward = averageReward;
                bestActionId = actionCandidateId;
            }
        }

        return bestActionId;
    }

    public void updateRewards(List<Integer> moveHistory, double reward) {
        for (int i = 0; i < moveHistory.size(); ++i) {
            String nGram = "";
            for (int j = i; j < Math.min(i + N, moveHistory.size()); ++j) {
                nGram = String.format("%02d", moveHistory.get(j)) + nGram;
                putRewardInTable(nGram, reward);
            }
        }
    }

    public void decayMoves() {
        for (var nGram : nGramResultsMapping.keySet()) {
            Result decayedResult = nGramResultsMapping.get(nGram);
            decayedResult.totalReward -= (decayedResult.totalReward - MCTree.BASE_REWARD * decayedResult.visitCount) * DECAY_FACTOR;
            decayedResult.visitCount *= (1 - DECAY_FACTOR);

            nGramResultsMapping.put(nGram, decayedResult);
        }
    }

    private void putRewardInTable(String nGram, double reward) {
        if (nGramResultsMapping.containsKey(nGram)) {
            Result result = nGramResultsMapping.get(nGram);
            result.totalReward += reward;
            result.visitCount += 1;
            nGramResultsMapping.put(nGram, result);
        } else {
            nGramResultsMapping.put(nGram, new Result(reward));
        }
    }

    private double getAverageRewardByAction(List<Integer> moveHistory, Integer actionCandidate) {
        String nGram = "";
        nGram = String.format("%02d", actionCandidate) + nGram;

        Result reward;
        double totalReward;
        double totalVisitCount;

        if (nGramResultsMapping.containsKey(nGram)) {
            reward = nGramResultsMapping.get(nGram);
            totalReward = reward.totalReward;
            totalVisitCount = reward.visitCount;
        } else {
            return MCTree.MAX_REWARD;
        }

        for (int i = moveHistory.size() - 1; i >= 0; --i) {
            Integer action = moveHistory.get(i);
            nGram = String.format("%02d", action) + nGram;

            if (nGram.length() / 2 > N) {
                break;
            }

            if (nGramResultsMapping.containsKey(nGram)) {
                if (nGramResultsMapping.get(nGram).visitCount >= k) {
                    reward = nGramResultsMapping.get(nGram);
                    totalReward += reward.totalReward;
                    totalVisitCount += reward.visitCount;
                }
            } else {
                return MCTree.MAX_REWARD;
            }
        }

        return totalReward / totalVisitCount;
    }
}
