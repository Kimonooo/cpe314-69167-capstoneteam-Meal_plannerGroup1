package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ChartMathTest {

    @Test
    void smallCountsUseStepsOfOne() {
        assertEquals(1, ChartMath.niceStep(0));
        assertEquals(1, ChartMath.niceStep(3));
        assertEquals(1, ChartMath.niceStep(5));
    }

    @Test
    void biggerCountsUseTidySteps() {
        assertEquals(2, ChartMath.niceStep(6));
        assertEquals(10, ChartMath.niceStep(50));
        assertEquals(250, ChartMath.niceStep(1015));
    }

    @Test
    void upperBoundIsAlwaysAboveTheTallestBar() {
        assertEquals(4, ChartMath.upperBound(3));
        assertEquals(60, ChartMath.upperBound(50));
        assertEquals(1250, ChartMath.upperBound(1015));
        for (int max = 0; max < 5000; max += 7) {
            assertTrue(ChartMath.upperBound(max) > max, "max " + max);
            assertEquals(0, ChartMath.upperBound(max) % ChartMath.niceStep(max), "max " + max);
        }
    }

    @Test
    void negativeNumbersAreTreatedAsZero() {
        assertEquals(1, ChartMath.niceStep(-10));
        assertEquals(1, ChartMath.upperBound(-10));
    }
}
