package com.mycompany.app;

import com.mycompany.app.engine.core.*;
import com.mycompany.app.utils.RNG;
import me.tongfei.progressbar.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

public class GenerateLevel {
    private static final int GENERATED_LEVEL_WIDTH = 150;
    private static final int GENERATED_LEVEL_HEIGHT = 16;
    private static final int LEVEL_COUNT = 20;
    private static final String LEVEL_DIR = "./levels/thesisTestLevels100/";
    private static final int REPETITION_COUNT = 15;
    private static final int TIME_FOR_LEVEL = 20;
    private static final int DISTANCE_MULTIPLIER = 16;
    public static final int RANDOM_SEED = 12345;
    public static final boolean TEST_DETERMINISM = false;

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

        for (int i = 0; i < REPETITION_COUNT; ++i) {
            RNG.setSeed(RANDOM_SEED);
            MarioAgent agent = new com.mycompany.app.agents.robinBaumgarten.Agent();

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

    public static String generateSampleLevels() {
        int generated = 0;
//        var progressBar = new ProgressBar("Levels", LEVEL_COUNT);

        Path dir = Paths.get(LEVEL_DIR);

        try {
            if (Files.exists(dir) && Files.isDirectory(dir)) {
                Stream<Path> fileStream = Files.list(dir);
                fileStream
                        .map(Path::toFile)
                        .forEach(File::delete);

                fileStream.close();
            } else {
                Files.createDirectory(dir);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (generated < LEVEL_COUNT) {
            try {
                MarioLevelGenerator generator = new com.mycompany.app.levelGenerators.sampler.LevelGenerator();
                String level = generator.getGeneratedLevel(new MarioLevelModel(GENERATED_LEVEL_WIDTH, GENERATED_LEVEL_HEIGHT), new MarioTimer(5 * 60 * 60 * 1000));

                if (TEST_DETERMINISM) {
                    testDeterminism(level);
                }

                Path file = Paths.get(String.format(LEVEL_DIR + "lvl-%02d.txt", generated + 1));
                if (!Files.exists(file)) {
                    Files.createFile(file);
                }
                Files.write(file, level.getBytes());

                generated++;
            } catch (IllegalArgumentException | IOException e) {
                System.out.println("Error during generation. Generating again.");
                e.printStackTrace();
            }
//            finally {
//                progressBar.step();
//            }
        }

        return LEVEL_DIR;
    }

    public static void main(String[] args) throws IOException {
        var time = System.currentTimeMillis();

        generateSampleLevels();

        System.out.println("--------------------------------------------------------------------");
        System.out.println("Execution time: " + (System.currentTimeMillis() - time));
    }
}
