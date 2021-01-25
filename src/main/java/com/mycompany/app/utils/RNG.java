package com.mycompany.app.utils;

import java.util.Random;

public class RNG {
    private static final Random random = new Random(System.currentTimeMillis());

    public static void setSeed(long seed) {
        random.setSeed(seed);
    }

    public static int nextInt(int upperBound) {
        var res = random.nextInt(upperBound);
//        System.out.println(res);
        return res;
    }
}
