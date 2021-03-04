package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.helper.GameStatus;

public class Utils {
    // Left, Right, Down, Speed, Jump
    public static boolean[][] availableActions = new boolean[][]{
            new boolean[]{false, true, false, false, false},
            new boolean[]{false, true, false, true, false},
            new boolean[]{false, true, false, false, true},
            new boolean[]{false, true, false, true, true},
            new boolean[]{true, false, false, false, false},
            new boolean[]{true, false, false, true, false},
            new boolean[]{true, false, false, false, true},
            new boolean[]{true, false, false, true, true},
    };

    public static double calcReward(MarioForwardModel startSnapshot, MarioForwardModel endSnapshot, int currentDepth) {
        if (endSnapshot.getGameStatus() != GameStatus.RUNNING) {
            // If it is Game Over, there is either win or lose.
            return endSnapshot.getGameStatus() == GameStatus.WIN ? MCTree.MAX_REWARD : MCTree.MIN_REWARD;
        }

        double startX = startSnapshot.getMarioFloatPos()[0];
        double endX = endSnapshot.getMarioFloatPos()[0];
        int damage = Math.max(0, startSnapshot.getMarioMode() - endSnapshot.getMarioMode()) +
                Math.max(0, startSnapshot.getNumLives() - endSnapshot.getNumLives());

        double reward = 0.5 +
                MCTree.PROGRESS_WEIGHT * (endX - startX) / (11.0 * (1 + MCTree.MAX_SIMULATION_DEPTH))
//                        + PATH_LENGTH_WEIGHT * (maxTreeDepth - currentDepth) / maxTreeDepth
                        - MCTree.DAMAGE_WEIGHT * damage;

        return reward;
    }
}
