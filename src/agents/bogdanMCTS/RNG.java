package agents.bogdanMCTS;

import java.util.Random;

public class RNG {
    private static Random random;

    public static void createRNG() {
        random = new Random();
    }

    public static void createRNG(int seed) {
        random = new Random(seed);
    }

    public static int nextInt(int upperBound) {
        return random.nextInt(upperBound);
    }
}
