package com.mycompany.app.agents.random;

import java.util.ArrayList;
import java.util.Random;

import com.mycompany.app.engine.core.MarioAgent;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.utils.RNG;

public class Agent implements MarioAgent {
    private ArrayList<boolean[]> choices;

    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {
        choices = new ArrayList<>();
        //right run
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        choices.add(new boolean[]{false, true, false, true, false});
        //right jump and run
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        choices.add(new boolean[]{false, true, false, true, true});
        // right
        choices.add(new boolean[]{false, true, false, false, false});
        choices.add(new boolean[]{false, true, false, false, false});
        choices.add(new boolean[]{false, true, false, false, false});
        choices.add(new boolean[]{false, true, false, false, false});
        // right jump
        choices.add(new boolean[]{false, true, false, false, true});
        choices.add(new boolean[]{false, true, false, false, true});
        choices.add(new boolean[]{false, true, false, false, true});
        choices.add(new boolean[]{false, true, false, false, true});
        //left
        choices.add(new boolean[]{true, false, false, false, false});
        //left run
        choices.add(new boolean[]{true, false, false, true, false});
        //left jump
        choices.add(new boolean[]{true, false, false, false, true});
        //left jump and run
        choices.add(new boolean[]{true, false, false, true, true});
    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        return choices.get(RNG.nextInt(choices.size()));
    }

    @Override
    public String getAgentName() {
        return "RandomAgent";
    }

}
