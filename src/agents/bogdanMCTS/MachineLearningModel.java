package agents.bogdanMCTS;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public interface MachineLearningModel {
    public void setHyperParameters(HashMap<Integer, Number> hyperParameters);
    public HashMap<Integer, List<Number>> getHyperParameterGrid();
}
