package com.mycompany.app;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.mycompany.app.agents.bogdanMCTS.MCTSEnhancements;
import com.mycompany.app.agents.bogdanMCTS.PaperAgent;
import com.mycompany.app.engine.core.*;
import com.mycompany.app.utils.Score;
import com.mycompany.app.utils.ThreadPool;
import me.tongfei.progressbar.ProgressBar;

public class PlayLevel {
    public static final int DISTANCE_MULTIPLIER = 16;
    public static final int TIME_FOR_LEVEL = 30;
    public static final int MARIO_START_MODE = 0;
    public static final String LEVEL_DIR = "./levels/thesisTestLevels100/";
//    public static final String LEVEL_DIR = "./levels/original/";
    public static final int NUMBER_OF_SAMPLES = 1;
    public static final int LEVEL_REPETITION_COUNT = 3;
    public static final Boolean VISUALIZATION = false;
    public static final Boolean LOAD_RESULTS_TO_GIT = true;

    private static final ArrayList<Future<Score>> futures = new ArrayList<>();
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

    private static void playLevel(PaperAgent agent, String levelName, int mctsEnhancementMask) {
        int levelWidth;
        levelWidth = calcLevelWidth(levelName);

        if (levelWidth <= 0) {
            agent.addResult(new Score(0.0f, 0.0f));
        }

        MarioGame game = new MarioGame();
        boolean canBeMultithreaded = !VISUALIZATION
                && !MCTSEnhancements.MaskContainsEnhancement(mctsEnhancementMask, MCTSEnhancements.Enhancement.WU_UCT);

        if (canBeMultithreaded) {
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
                return new Score(score, time);
            }));
        } else {
            var result = game.runGame(agent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
//            var result = game.runGame(new com.mycompany.app.agents.human.Agent(), getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
//            var score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
            var score = result.getCompletionPercentage();
            var time = (double) result.getRemainingTime() / 1000;
            agent.addResult(new Score(score, time));
//            progressBar.step();

            progressBar.step();
        }
    }

    private static void playListOfLevels(PaperAgent agent, List<String> levels, int levelCount, int mctsEnhancementMask) {
//        progressBar = new ProgressBar("Levels", levels.size());

        for (int i = 0; i < Math.min(levels.size(), levelCount); i++) {
            String levelPath = levels.get(i);

            for (int j = 0; j < LEVEL_REPETITION_COUNT; ++j) {
                playLevel(agent, levelPath, mctsEnhancementMask);
            }
        }
    }

    private static void playAllFolderLevels(PaperAgent agent, final String levelFolder, int levelCount, int mctsEnhancementMask) {
        // Play all levels from a level folder
        ArrayList<String> levels = new ArrayList<>();
        try {
            Files.list(Paths.get(levelFolder))
                    .forEach((x) -> levels.add(x.toString()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        playListOfLevels(agent, levels, levelCount, mctsEnhancementMask);
    }

    private static void playSingleLevel(PaperAgent agent, final String levelPath, final int playRepetitionCount, int mctsEnhancementMask) {
        progressBar = new ProgressBar("Repetitions", playRepetitionCount);

        for (int i = 0; i < playRepetitionCount; ++i) {
//                NodePool.createPool();
            playLevel(agent, levelPath, mctsEnhancementMask);
        }
    }

    private static void printStatistics(PaperAgent agent, long time, int enhancements) {
        System.out.println("--------------------------------------------------------------------");
        System.out.println("Average score for " + agent.getAgentName() + MCTSEnhancements.enhancementsToString(enhancements) + ": " + agent.averageScore());
//        System.out.println("Average time left for " + agent.getAgentName() + ": " + agent.averageTime());
        System.out.println("--------------------------------------------------------------------");
        System.out.println("Execution time: " + (System.currentTimeMillis() - time));
    }

    public static void main(String[] args) {
        var time = System.currentTimeMillis();

        PaperAgent mctsAgent = new com.mycompany.app.agents.bogdanMCTS.Agent();
        PaperAgent aStarAgent = new com.mycompany.app.agents.robinBaumgarten.Agent();

//        HashSet<Integer> availableEnhancementMasks = MCTSEnhancements.AvailableEnhancementMasks();
        MCTSEnhancements.Enhancement[] enList = MCTSEnhancements.Enhancement.values();
        ArrayList<Integer> availableEnhancementMasks = new ArrayList<>() {{
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[1], enList[2], enList[3], enList[4], enList[5], enList[6], enList[7], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[1], enList[2], enList[4]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[1], enList[3], enList[7]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[1], enList[5], enList[6], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[2], enList[3], enList[6]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[2], enList[5], enList[7]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[3], enList[4], enList[5], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[0], enList[4], enList[6], enList[7]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[1], enList[2], enList[3], enList[5]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[1], enList[2], enList[6], enList[7], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[1], enList[3], enList[4], enList[6], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[1], enList[4], enList[5], enList[7]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[2], enList[3], enList[4], enList[7], enList[8]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[2], enList[4], enList[5], enList[6]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[3], enList[5], enList[6], enList[7]}));
            add(MCTSEnhancements.AddEnhancements(0, new MCTSEnhancements.Enhancement[]{enList[8]}));
        }};
        System.out.println("Available MCTS enhancements combinations: "
                + availableEnhancementMasks.size());

//        for (int mctsEnhancementMask : availableEnhancementMasks) {
//            PlayAllSamplesWithEnhancements(mctsAgent, mctsEnhancementMask);
//            printStatistics(mctsAgent, time, mctsEnhancementMask);
//            time = System.currentTimeMillis();
//        }

        int enhancements = PresetEnhancements(mctsAgent);
        PlayAllSamples(mctsAgent, enhancements);
        printStatistics(mctsAgent, time, enhancements);
        mctsAgent.outputScores(NUMBER_OF_SAMPLES, LEVEL_REPETITION_COUNT, enhancements, LOAD_RESULTS_TO_GIT);

        PlayAllSamples(aStarAgent, 0);
        printStatistics(aStarAgent, time, 0);
        aStarAgent.outputScores(NUMBER_OF_SAMPLES, LEVEL_REPETITION_COUNT, 0, LOAD_RESULTS_TO_GIT);

//        playSingleLevel(aStarAgent, "./levels/lvl-killer_plant.txt", LEVEL_REPETITION_COUNT, 0);
//        playSingleLevel(aStarAgent, "./levels/original/lvl-1.txt", LEVEL_REPETITION_COUNT, 0);
//        playSingleLevel(aStarAgent, "./levels/original/lvl-4.txt", LEVEL_REPETITION_COUNT, 0);

        System.exit(0);
    }

    private static int PresetEnhancements(PaperAgent mctsAgent) {
        if (mctsAgent instanceof com.mycompany.app.agents.bogdanMCTS.Agent
                enhancedMCTSAgent) {
            int mctsEnhancementMask = MCTSEnhancements.
                    AddEnhancements(0, new MCTSEnhancements.Enhancement[] {
                        MCTSEnhancements.Enhancement.HARD_PRUNING,
                            MCTSEnhancements.Enhancement.WU_UCT,
//                            MCTSEnhancements.Enhancement.TREE_REUSE,
                            MCTSEnhancements.Enhancement.MIXMAX,
//                            MCTSEnhancements.Enhancement.N_GRAM_SELECTION,
//                            MCTSEnhancements.Enhancement.LOSS_AVOIDANCE,
//                            MCTSEnhancements.Enhancement.AGING,
//                            MCTSEnhancements.Enhancement.MACRO_ACTIONS,
//                            MCTSEnhancements.Enhancement.SP_MCTS
                    });

            enhancedMCTSAgent.setEnhancements(mctsEnhancementMask);

            return mctsEnhancementMask;
        }

        return 0;
    }

    private static void PlayAllSamplesWithEnhancements(PaperAgent mctsAgent, int mctsEnhancementMask) {
        if (mctsAgent instanceof com.mycompany.app.agents.bogdanMCTS.Agent
                enhancedMCTSAgent) {
            enhancedMCTSAgent.setEnhancements(mctsEnhancementMask);
        }

        PlayAllSamples(mctsAgent, mctsEnhancementMask);

        mctsAgent.outputScores(NUMBER_OF_SAMPLES, LEVEL_REPETITION_COUNT, mctsEnhancementMask, LOAD_RESULTS_TO_GIT);
    }

    private static void PlayAllSamples(PaperAgent agent, int mctsEnhancementMask) {
        agent.clearScores();

        int levelCount = GenerateLevel.LEVEL_COUNT;

        progressBar = new ProgressBar("Levels", levelCount * NUMBER_OF_SAMPLES * LEVEL_REPETITION_COUNT);

        for (int i = 0; i < NUMBER_OF_SAMPLES; ++i) {
//            playAllFolderLevels(agent, GenerateLevel.generateSampleLevels());
            playAllFolderLevels(agent, LEVEL_DIR, levelCount, mctsEnhancementMask);

            if (futures.size() > 0) {
                WaitForAllThreadLevelsToFinish(agent);
            }
        }

        progressBar.close();
    }

    private static void WaitForAllThreadLevelsToFinish(PaperAgent agent) {
        for (var future : futures) {
            try {
                Score score = future.get();
                agent.addResult(score);
                progressBar.step();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        futures.clear();
    }
}
