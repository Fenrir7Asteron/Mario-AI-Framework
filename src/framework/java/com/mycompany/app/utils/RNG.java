package com.mycompany.app.utils;

import java.util.Random;

public class RNG {
    private static final Random random = new Random(System.currentTimeMillis());

    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    public static int nextInt(int upperBound) {
        return random.nextInt(upperBound);
    }

    public static float nextFloat() {
        return random.nextFloat();
    }
}
