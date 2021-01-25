package com.mycompany.app;

import com.mycompany.app.engine.core.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class GenerateLevel {
    private static final int GENERATED_LEVEL_WIDTH = 150;
    private static final int GENERATED_LEVEL_HEIGHT = 16;
    private static final int LEVEL_COUNT = 100;
    private static final String LEVEL_DIR = "./levels/thesisTestLevels/";
    private static final Boolean VISUALIZATION = true;

    private static final int NUMBER_REPEATITIONS = 20;
    private static final int TIME_FOR_LEVEL = 10;
    private static final int DISTANCE_MULTIPLIER = 16;

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

    private static void testDeterminism(String levelName) {
        double prevScore = -1;
        double prevTime = -1;
        MarioGame game = new MarioGame();
        MarioResult result;

        for (int i = 0; i < NUMBER_REPEATITIONS; ++i) {
            MarioAgent agent = new com.mycompany.app.agents.random.Agent(i);

            result = game.runGame(agent, levelName, TIME_FOR_LEVEL, 0, false);
            double score = result.getCompletionPercentage() * GENERATED_LEVEL_WIDTH * DISTANCE_MULTIPLIER;
            double time = (double) result.getRemainingTime() / 1000;
            if (prevScore > -1 && prevTime > -1 &&
                    (Math.abs(score - prevScore) > 1e-6 || Math.abs(time - prevTime) > 1e-6)) {
                throw new Error("The game is stochastic!!! " +
                        "PrevScore: " + prevScore + " CurrScore: " + score +
                        " PrevTime: " + prevTime + " CurrTime: " + time);
            }
            prevScore = score;
            prevTime = time;
        }
    }

    private static void generateManyLevels() throws IOException {
        int generated = 0;
        while (generated < LEVEL_COUNT) {
            try {
                MarioLevelGenerator generator = new com.mycompany.app.levelGenerators.sampler.LevelGenerator();
                String level = generator.getGeneratedLevel(new MarioLevelModel(GENERATED_LEVEL_WIDTH, GENERATED_LEVEL_HEIGHT), new MarioTimer(5 * 60 * 60 * 1000));
                Path dir = Paths.get(LEVEL_DIR);
                if (!Files.exists(dir)) {
                    Files.createDirectory(dir);
                }
                Path file = Paths.get(String.format(LEVEL_DIR + "lvl-%d.txt", generated + 1));
                Files.write(file, level.getBytes());

                generated++;
            } catch (IllegalArgumentException e) {
                System.out.println("Error during generation. Generating again.");
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) throws IOException {
        generateManyLevels();
    }
}
