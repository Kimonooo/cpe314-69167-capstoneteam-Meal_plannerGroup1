package com.platenplatter.util;

import java.util.stream.LongStream;

/** Picks tidy numbers for the y-axis of the chart (1, 2, 5, 10, 20, 25, 50, ...). */
public final class ChartMath {
    private ChartMath() { }

    /** The smallest tidy step that splits the axis into about 5 parts. Negative numbers count as 0. */
    public static int niceStep(long max) {
        long rough = (long) Math.ceil(Math.max(max, 0) / 5.0);
        if (rough <= 1) {
            return 1;
        }
        return (int) LongStream.iterate(1, power -> power * 10).limit(10)          // 1, 10, 100, ...
                .flatMap(power -> LongStream.of(power, 2 * power, power >= 10 ? 5 * power / 2 : 0, 5 * power))
                .filter(candidate -> candidate >= rough)
                .findFirst()
                .orElse(rough);
    }

    /** The top of the axis: one tidy step above the biggest bar. */
    public static int upperBound(long max) {
        int step = niceStep(max);
        return (int) ((Math.max(max, 0) / step + 1) * step);
    }
}
