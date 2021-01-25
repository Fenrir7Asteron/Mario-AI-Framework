package com.mycompany.app;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.mycompany.app.agents.bogdanMCTS.Agent;
import com.mycompany.app.agents.bogdanMCTS.MachineLearningModel;
import com.mycompany.app.engine.core.*;
import com.mycompany.app.utils.Score;
import com.mycompany.app.utils.ThreadPool;

public class PlayLevel {
    public static final int DISTANCE_MULTIPLIER = 16;
    public static final int TIME_FOR_LEVEL = 40;
    public static final int MARIO_START_MODE = 0;
    public static final String LEVEL_DIR = "./levels/thesisTestLevels/";
    public static final int PLAY_REPETITION_COUNT = 100;
    public static final Boolean VISUALIZATION = false;
    public static final Boolean MULTITHREADED = true;

    private static ArrayList<Future<?>> futures = new ArrayList<>();

    public static void printResults(MarioResult result) {
        System.out.println("****************************************************************");
        System.out.println("Game Status: " + result.getGameStatus().toString() +
                " Percentage Completion: " + result.getCompletionPercentage());
        System.out.println("Lives: " + result.getCurrentLives() + " Coins: " + result.getCurrentCoins() +
                " Remaining Time: " + (int) Math.ceil(result.getRemainingTime() / 1000f));
        System.out.println("Mario State: " + result.getMarioMode() +
                " (Mushrooms: " + result.getNumCollectedMushrooms() + " Fire Flowers: " + result.getNumCollectedFireflower() + ")");
        System.out.println("Total Kills: " + result.getKillsTotal() + " (Stomps: " + result.getKillsByStomp() +
                " Fireballs: " + result.getKillsByFire() + " Shells: " + result.getKillsByShell() +
                " Falls: " + result.getKillsByFall() + ")");
        System.out.println("Bricks: " + result.getNumDestroyedBricks() + " Jumps: " + result.getNumJumps() +
                " Max X Jump: " + result.getMaxXJump() + " Max Air Time: " + result.getMaxJumpAirTime());
        System.out.println("****************************************************************");
    }

    public static String getLevel(String filepath) {
        String content = "";
        try {
            content = new String(Files.readAllBytes(Paths.get(filepath)));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    private static int calcLevelWidth(String levelName) {
        List<String> lines = null;

        try {
            lines = Files.readAllLines(Paths.get(levelName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        assert lines != null;
        if (!lines.isEmpty()) {
            return lines.get(0).length();
        }
        return 0;
    }

    private static void playLevel(Agent agent, String levelName) {
        int levelWidth;
        levelWidth = calcLevelWidth(levelName);

        if (levelWidth <= 0) {
            agent.addResult(new Score(0.0f, 0.0f));
        }

        MarioGame game = new MarioGame();

        if (MULTITHREADED) {
            Agent threadAgent = null;
            try {
                threadAgent = (Agent) agent.clone();
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            // Play level in a different thread. The option is off when the play is visualized.
            Agent finalThreadAgent = threadAgent;
            futures.add(ThreadPool.parallelGamesThreadPool.submit(() -> {
                var result = game.runGame(finalThreadAgent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
                var score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
                var time = (double) result.getRemainingTime() / 1000;
                agent.addResult(new Score(score, time));
            }));
        } else {
            var result = game.runGame(agent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
            var score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
            var time = (double) result.getRemainingTime() / 1000;
            agent.addResult(new Score(score, time));
        }
    }

    private static void tuneModels(List<Agent> agents) {
        for (var agent : agents) {
            if (!(agent instanceof MachineLearningModel)) {
                continue;
            }
            // Do something
        }
    }

    private static void playListOfLevels(List<Agent> agents, List<String> levels) {
        for (var levelPath : levels) {
            for (var agent : agents) {
                playLevel(agent, levelPath);
            }
        }
    }

    private static void playAllFolderLevels(List<Agent> agents, final String levelFolder) {
        // Play all levels from a level folder
        ArrayList<String> levels = new ArrayList<>();
        try {
            Files.list(Paths.get(levelFolder))
                    .forEach((x) -> levels.add(x.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        playListOfLevels(agents, levels);
    }

    private static void playSingleLevel(List<Agent> agents, final String levelPath, final int playRepetitionCount) {
        for (int i = 0; i < playRepetitionCount; ++i) {
            for (var agent : agents) {
                playLevel(agent, levelPath);
            }
        }
    }

    private static void printStatistics(List<Agent> agents) {
        for (var agent : agents) {
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Average score for " + agent.getAgentName() + ": " + agent.averageScore());
            System.out.println("Average time left for " + agent.getAgentName() + ": " + agent.averageTime());
        }
    }

    public static void main(String[] args) {
        var time = System.currentTimeMillis();

        List<Agent> agents = new ArrayList<>();
        agents.add(new com.mycompany.app.agents.bogdanMCTS.Agent());

        tuneModels(agents); // Grid search hyperparameters

        playAllFolderLevels(agents, LEVEL_DIR);

//        playSingleLevel(agents, "./levels/lvl-killer_plant.txt", PLAY_REPETITION_COUNT);

        for (var future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        printStatistics(agents);

        System.out.println("--------------------------------------------------------------------");
        System.out.println("Execution time: " + (System.currentTimeMillis() - time));

        for (var agent : agents) {
            agent.outputScores();
        }
    }
}
