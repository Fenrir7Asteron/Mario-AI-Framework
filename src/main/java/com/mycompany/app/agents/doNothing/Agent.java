package com.mycompany.app.agents.doNothing;

import com.mycompany.app.engine.core.MarioAgent;
import com.mycompany.app.engine.core.MarioForwardModel;
import com.mycompany.app.engine.core.MarioTimer;
import com.mycompany.app.engine.helper.MarioActions;

public class Agent implements MarioAgent {
    @Override
    public void initialize(MarioForwardModel model, MarioTimer timer) {

    }

    @Override
    public boolean[] getActions(MarioForwardModel model, MarioTimer timer) {
        return new boolean[MarioActions.numberOfActions()];
    }

    @Override
    public String getAgentName() {
        return "DoNothingAgent";
    }
}
