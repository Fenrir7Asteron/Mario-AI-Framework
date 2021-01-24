package utils;

import java.util.List;

public class MyMath {
    public static double average(List<Double> list) {
        var sum = list.stream().reduce(Double::sum);
        return sum.map(x -> x / list.size()).orElse(0.0);
    }
}
