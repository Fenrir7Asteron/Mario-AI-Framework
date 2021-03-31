package com.mycompany.app.agents.bogdanMCTS;

import com.mycompany.app.engine.core.MarioAgent;
import com.mycompany.app.utils.Score;

import java.io.FileOutputStream;
import java.io.IOException;

import static com.mycompany.app.utils.MyMath.average;

public interface PaperAgent extends MarioAgent  {
    public void addResult(Score newScore);
    public double averageScore();
    public double averageTime();
    public void outputScores(long levelCount);
}
