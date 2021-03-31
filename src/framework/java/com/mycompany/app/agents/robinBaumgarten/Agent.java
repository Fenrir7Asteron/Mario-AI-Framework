package com.mycompany.app.agents.robinBaumgarten;

import com.mycompany.app.agents.bogdanMCTS.PaperAgent;
import com.mycompany.app.engine.core.MarioAgent;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.engine.helper.MarioActions;
import com.mycompany.app.utils.Score;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import static com.mycompany.app.utils.MyMath.average;

/**
 * @author RobinBaumgarten
 */
public class Agent implements PaperAgent {
    public static final String DATA_FOLDER = "data/";

    private boolean[] action;
    private AStarTree tree;
    private ArrayList<Double> resultScores = new ArrayList<>();
    private ArrayList<Double> resultTimes = new ArrayList<>();

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        this.action = new boolean[MarioActions.numberOfActions()];
        this.tree = new AStarTree();
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        action = this.tree.optimise(model, timer);
        return action;
    }

    @Override
    public String getAgentName() {
        return "RobinBaumgartenAgent";
    }

    @Override
    public void addResult(Score newScore) {
        resultScores.add(newScore.score);
        resultTimes.add(newScore.time);
    }

    @Override
    public double averageScore() {
        return average(resultScores);
    }

    @Override
    public double averageTime() {
        return average(resultTimes);
    }

    @Override
    public void outputScores(long levelCount) {
        var namePrefix = getAgentName() + levelCount;

        try(FileOutputStream fos = new FileOutputStream(DATA_FOLDER + namePrefix + ".txt"))
        {
            for (var score : resultScores) {
                byte[] buffer = (score.toString() + "\n").getBytes();

                fos.write(buffer, 0, buffer.length);
            }
        }
        catch(IOException ex){
            ex.printStackTrace();
        }

        System.out.println("--------------------------------------------------------------------");
        System.out.println("Agent [" + namePrefix + "]: The file has been written");
    }
}
