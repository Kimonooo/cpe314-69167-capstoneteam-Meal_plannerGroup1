package com.platenplatter.util;

/** Picks tidy numbers for the chart's side scale (0, 250, 500...) so labels never show 0.5 meals. */
public final class ChartMath {
    private ChartMath() { }

    /** The gap between two tick marks, chosen from 1, 2, 5, 10, 20, 25, 50, 100, 200, 250, 500... */
    public static int niceStep(int max) {
        int rough = (int) Math.ceil(Math.max(max, 0) / 5.0);
        if (rough <= 1) {
            return 1;
        }
        for (long power = 1; power < 1_000_000_000L; power *= 10) {
            long[] candidates = {power, 2 * power, power >= 10 ? 5 * power / 2 : 0, 5 * power};
            for (long candidate : candidates) {
                if (candidate >= rough) {
                    return (int) candidate;
                }
            }
        }
        return rough;
    }

    /** The top of the scale: always a little higher than the tallest bar. */
    public static int upperBound(int max) {
        int step = niceStep(max);
        return (Math.max(max, 0) / step + 1) * step;
    }
}
