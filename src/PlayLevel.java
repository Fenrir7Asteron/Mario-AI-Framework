import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import agents.bogdanMCTS.Agent;
import agents.bogdanMCTS.MCTree;
import agents.bogdanMCTS.MachineLearningModel;
import engine.core.*;

public class PlayLevel {
    private static final int DISTANCE_MULTIPLIER = 16;
    private static final int TIME_FOR_LEVEL = 40;
    private static final int MARIO_START_MODE = 0;
    private static final String LEVEL_DIR = "./levels/thesisTestLevels/";
    private static final int LEVEL_COUNT = 100;
    private static final Boolean VISUALIZATION = true;

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
        }
        return content;
    }

    private static int calcLevelWidth(String levelName) throws IOException {
        var lines = Files.readAllLines(Paths.get(levelName));
        if (!lines.isEmpty()) {
            return lines.get(0).length();
        }
        return 0;
    }

    private static double averageScore = 0;
    private static double averageTimeLeft = 0;

    private static void playLevel(MarioAgent agent, String levelName) throws IOException {
        int levelWidth;
        levelWidth = calcLevelWidth(levelName);

        if (levelWidth > 0) {
            MarioGame game = new MarioGame();
            MarioResult result;
            if (agent instanceof MachineLearningModel) {
                ((Agent) agent).setHyperParameters(new HashMap<>(Map.of(
                        Agent.Hyperparameter.EXPLORATION_FACTOR.ordinal(), 0.188,
                        Agent.Hyperparameter.MIXMAX_MAX_FACTOR.ordinal(), 0.125,
                        Agent.Hyperparameter.MAX_DEPTH.ordinal(), 6
                )));
            }

            result = game.runGame(agent, getLevel(levelName), TIME_FOR_LEVEL, MARIO_START_MODE, VISUALIZATION);
            double score = result.getCompletionPercentage() * levelWidth * DISTANCE_MULTIPLIER;
            double time = (double) result.getRemainingTime() / 1000;
            System.out.println(levelName + ": Score = " + score + "; Time Left = " + time);
            averageScore += score;
            averageTimeLeft += time;
        }
    }

    private static void tuneModels(List<MarioAgent> agents) {
        for (var agent : agents) {
            if (!(agent instanceof MachineLearningModel)) {
                continue;
            }
            // Do something
        }
    }

    public static void main(String[] args) throws IOException {
        List<MarioAgent> agents = new ArrayList<>();
        agents.add(new agents.bogdanMCTS.Agent());
        agents.add(new agents.robinBaumgarten.Agent());

        tuneModels(agents); // Grid search hyperparameters

        // Play all levels from a level folder
        Files.list(Paths.get(LEVEL_DIR)).forEach((x) -> {
            try {
                playLevel(agents.get(0), x.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        System.out.println("Average score: " + averageScore / LEVEL_COUNT);
        System.out.println("Average time left: " + averageTimeLeft / LEVEL_COUNT);
//        playLevel("./levels/original/lvl-12.txt");
    }
}
