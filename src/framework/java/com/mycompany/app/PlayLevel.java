package com.mycompany.app;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.mycompany.app.agents.bogdanMCTS.MCTSEnhancements;
import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.agents.bogdanMCTS.PaperAgent;
import com.mycompany.app.engine.core.*;
import com.mycompany.app.utils.Score;
import com.mycompany.app.utils.ThreadPool;
import me.tongfei.progressbar.ProgressBar;

public class PlayLevel {
    public static final int DISTANCE_MULTIPLIER = 16;
    public static final int TIME_FOR_LEVEL = 2;
    public static final int MARIO_START_MODE = 0;
    public static final String LEVEL_DIR = "./levels/original/";
    public static final int NUMBER_OF_SAMPLES = 1;
    public static final int PLAY_REPETITION_COUNT = 300;
    public static final Boolean VISUALIZATION = true;
    public static final Boolean MULTITHREADED = false;

    private static ArrayList<Future<?>> futures = new ArrayList<>();
    private static ProgressBar progressBar;

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

    private static void playLevel(PaperAgent agent, String levelName) {
        int levelWidth;
        levelWidth = calcLevelWidth(levelName);

        if (levelWidth <= 0) {
            agent.addResult(new Score(0.0f, 0.0f));
        }

        MarioGame game = new MarioGame();

        if (MULTITHREADED) {
            PaperAgent threadAgent = null;
            try {
                Constructor constructor = agent.getClass().getConstructor();
                threadAgent = (PaperAgent) constructor.newInstance();
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                e.printStackTrace();
            }

            // Play level in a different thread. The option is off when the play is visualized.
            PaperAgent finalThreadAgent = threadAgent;
            futures.add(ThreadPool.parallelGamesThreadPool.submit(() -> {
                var result = game.runGame(finalThreadAgent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
//                var score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
                var score = result.getCompletionPercentage();
                var time = (double) result.getRemainingTime() / 1000;
                agent.addResult(new Score(score, time));
//                progressBar.step();
            }));
        } else {
            var result = game.runGame(agent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
//            var result = game.runGame(new com.mycompany.app.agents.human.Agent(), getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
//            var score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
            var score = result.getCompletionPercentage();
            var time = (double) result.getRemainingTime() / 1000;
            agent.addResult(new Score(score, time));
//            progressBar.step();
        }
    }

    private static void playListOfLevels(List<PaperAgent> agents, List<String> levels) {
//        progressBar = new ProgressBar("Levels", levels.size());

        for (var levelPath : levels) {
            for (var agent : agents) {
                playLevel(agent, levelPath);
            }
        }
    }

    private static void playAllFolderLevels(List<PaperAgent> agents, final String levelFolder) {
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

    private static void playSingleLevel(List<PaperAgent> agents, final String levelPath, final int playRepetitionCount) {
        progressBar = new ProgressBar("Repetitions", playRepetitionCount);

        for (int i = 0; i < playRepetitionCount; ++i) {
            for (var agent : agents) {
//                NodePool.createPool();
                playLevel(agent, levelPath);
            }
        }
    }

    private static void printStatistics(List<PaperAgent> agents) {
        for (var agent : agents) {
            System.out.println("--------------------------------------------------------------------");
            System.out.println("Average score for " + agent.getAgentName() + ": " + agent.averageScore());
            System.out.println("Average time left for " + agent.getAgentName() + ": " + agent.averageTime());
        }
    }

    public static void main(String[] args) {
        var time = System.currentTimeMillis();

        List<PaperAgent> agents = new ArrayList<>();
        agents.add(new com.mycompany.app.agents.bogdanMCTS.Agent());
        agents.add(new com.mycompany.app.agents.robinBaumgarten.Agent());

        HashSet<Integer> availableEnhancementMasks = MCTSEnhancements.AvailableEnhancementMasks();
        System.out.println("Available MCTS enhancements variants: "
                + availableEnhancementMasks.size());

        for (int mctsEnhancementMask : availableEnhancementMasks) {
            PlayAllSamples(time, agents, mctsEnhancementMask);
        }

//        playSingleLevel(agents, "./levels/lvl-killer_plant.txt", PLAY_REPETITION_COUNT);
//        playSingleLevel(agents, "./levels/original/lvl-1.txt", PLAY_REPETITION_COUNT);
//        playSingleLevel(agents, "./levels/original/lvl-4.txt", PLAY_REPETITION_COUNT);

        System.exit(0);
    }

    private static void PlayAllSamples(long time, List<PaperAgent> agents, int mctsEnhancementMask) {
        for (PaperAgent agent : agents) {
            if (agent instanceof com.mycompany.app.agents.bogdanMCTS.Agent
                    enhancedMCTSAgent) {
                enhancedMCTSAgent.setEnhancements(mctsEnhancementMask);
            }
        }

        progressBar = new ProgressBar("Samples", NUMBER_OF_SAMPLES);

        for (int i = 0; i < NUMBER_OF_SAMPLES; ++i) {
            playAllFolderLevels(agents, GenerateLevel.generateSampleLevels());
            progressBar.step();
        }

        if (MULTITHREADED) {
            for (var future : futures) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }

        printStatistics(agents);

        System.out.println("--------------------------------------------------------------------");
        System.out.println("Execution time: " + (System.currentTimeMillis() - time));

        for (var agent : agents) {
            agent.outputScores((int) progressBar.getMax(), mctsEnhancementMask);
        }

        progressBar.close();
    }
}
