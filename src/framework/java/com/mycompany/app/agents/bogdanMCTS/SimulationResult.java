package com.mycompany.app.agents.bogdanMCTS;

import java.util.LinkedList;
import java.util.List;

public class SimulationResult {
    public double reward;
    public List<Integer> moveHistory;

    public SimulationResult(double reward) {
        this.reward = reward;
        moveHistory = new LinkedList<>();
    }

    public SimulationResult(double reward, List<Integer> moveHistory) {
        this.reward = reward;
        this.moveHistory = moveHistory;
    }
}
