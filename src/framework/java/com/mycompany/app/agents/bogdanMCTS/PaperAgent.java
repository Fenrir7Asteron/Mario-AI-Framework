package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.engine.core.MarioAgent;
import com.mycompany.app.utils.Score;

public interface PaperAgent extends MarioAgent  {
    public void addResult(Score newScore);
    public double averageScore();
    public double averageTime();
    public void outputScores(int numberOfSamples, int levelRepetitions, int enhancements, Boolean loadResultsToGit);
    public void clearScores();
}
