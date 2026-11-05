package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakCalculatorTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4); // a Wednesday

    private static Set<LocalDate> days(int... daysAgo) {
        java.util.HashSet<LocalDate> set = new java.util.HashSet<>();
        for (int d : daysAgo) {
            set.add(TODAY.minusDays(d));
        }
        return set;
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
        // today is Wednesday 4 Nov; Monday is 2 Nov, Tuesday 3 Nov
        boolean[] flags = StreakCalculator.weekFlags(days(0, 2), TODAY);
        assertArrayEquals(new boolean[] {true, false, true, false, false, false, false}, flags);
    }

    @Test
    void bestStreakIgnoresDaysThatHaveNotHappenedYet() {
        // planned: today and the 5 days AFTER today, plus yesterday
        java.util.HashSet<LocalDate> set = new java.util.HashSet<>();
        for (int i = -1; i <= 5; i++) {
            set.add(TODAY.plusDays(i));
        }
        assertEquals(7, StreakCalculator.longest(set));          // every day counted
        assertEquals(2, StreakCalculator.longest(set, TODAY));   // only yesterday and today count
        assertEquals(0, StreakCalculator.longest(null, TODAY));
    }
}
