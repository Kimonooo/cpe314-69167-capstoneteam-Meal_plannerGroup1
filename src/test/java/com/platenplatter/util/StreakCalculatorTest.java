package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4); // a Wednesday

    private static Set<LocalDate> days(int... daysAgo) {
        return Arrays.stream(daysAgo).mapToObj(TODAY::minusDays).collect(Collectors.toSet());
    }

    @Test
    void noMealsMeansNoStreak() {
        assertEquals(0, StreakCalculator.current(Set.of(), TODAY));
        assertEquals(0, StreakCalculator.current(null, TODAY));
        assertEquals(0, StreakCalculator.longest(Set.of()));
    }

    @Test
    void countsDaysInARowEndingToday() {
        assertEquals(3, StreakCalculator.current(days(0, 1, 2), TODAY));
        assertEquals(1, StreakCalculator.current(days(0), TODAY));
    }

    @Test
    void aGapBreaksTheStreak() {
        assertEquals(2, StreakCalculator.current(days(0, 1, 3, 4, 5), TODAY));
    }

    @Test
    void streakStaysAliveIfOnlyTodayIsMissing() {
        Set<LocalDate> d = days(1, 2, 3);
        assertEquals(3, StreakCalculator.current(d, TODAY));
        assertTrue(StreakCalculator.atRisk(d, TODAY));
        assertFalse(StreakCalculator.atRisk(days(0, 1), TODAY));
    }

    @Test
    void twoMissedDaysResetTheStreakToZero() {
        assertEquals(0, StreakCalculator.current(days(2, 3, 4), TODAY));
        assertFalse(StreakCalculator.atRisk(days(2, 3, 4), TODAY));
    }

    @Test
    void longestFindsTheBestRunAnywhere() {
        assertEquals(4, StreakCalculator.longest(days(0, 1, 5, 6, 7, 8, 20)));
        assertEquals(1, StreakCalculator.longest(days(0, 5, 9)));
    }

    @Test
    void weekFlagsMarkTheDaysOfThisWeekMondayFirst() {
        // today is Wednesday 4 Nov; Monday is 2 Nov
        assertEquals(List.of(true, false, true, false, false, false, false), StreakCalculator.weekFlags(days(0, 2), TODAY));
    }

    @Test
    void bestStreakIgnoresDaysThatHaveNotHappenedYet() {
        // planned: yesterday, today and the 5 days AFTER today
        Set<LocalDate> set = IntStream.rangeClosed(-1, 5).mapToObj(TODAY::plusDays).collect(Collectors.toSet());
        assertEquals(7, StreakCalculator.longest(set));          // every day counted
        assertEquals(2, StreakCalculator.longest(set, TODAY));   // only yesterday and today count
        assertEquals(0, StreakCalculator.longest(null, TODAY));
    }
}
