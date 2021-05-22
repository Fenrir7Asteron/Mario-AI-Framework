package com.mycompany.app.agents.bogdanMCTS.Enchancements;

import com.mycompany.app.agents.bogdanMCTS.MCTree;
import com.mycompany.app.engine.core.MarioForwardModel;

import java.util.HashMap;

public class ProcrastinationPunisher implements Cloneable {
    public float CELL_WIDTH = 10.0f;
    public float SCORE_DECREASE = 0.01f;
    public HashMap<String, Integer> cellVisitCounter = new HashMap<>();

    public synchronized double decreaseRewardBasedOnProcrastination(MarioForwardModel model, double reward) {
        Cell cell = getCell(model);
        int visitCount = cellVisitCounter.getOrDefault(cell.toString(), 0);
        return Math.max(MCTree.MIN_REWARD, reward - visitCount * SCORE_DECREASE);
    }

    public synchronized void addPositionSnapshot(MarioForwardModel model) {
        Cell cell = getCell(model);
        cellVisitCounter.put(cell.toString(),
                cellVisitCounter.getOrDefault(cell.toString(), 0) + 1);
    }

    private Cell getCell(MarioForwardModel model) {
        float x = model.getMarioFloatPos()[0];
        float y = model.getMarioFloatPos()[1];
        int cellX = (int) (x / CELL_WIDTH);
        int cellY = (int) (y / CELL_WIDTH);
        return new Cell(cellX, cellY);
    }

    public void clear() {
        cellVisitCounter.clear();
    }

    @Override
    public Object clone() {
        ProcrastinationPunisher newPunisher = new ProcrastinationPunisher();
        newPunisher.cellVisitCounter = (HashMap<String, Integer>) this.cellVisitCounter.clone();
        return newPunisher;
    }

    private static class Cell {
        public int x;
        public int y;

        public Cell(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public String toString() {
            return "(" + x + " " + y + ")";
//            return "(" + x + ")";
        }
    }
}
