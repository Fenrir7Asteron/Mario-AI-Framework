package agents.bogdanMCTS.Enchancements;

import agents.bogdanMCTS.MCTree;

public class MixMax {
    private final static double MIXMAX_MAX_FACTOR = 0.125f;

    public static double getExploitation(final double averageReward, final double maxReward) {
        return MIXMAX_MAX_FACTOR * maxReward + (1.0f - MIXMAX_MAX_FACTOR) * averageReward;
    }
}
