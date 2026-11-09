package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class StreakBadgeTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private static PlannedMeal meal(int prep) {
        return new PlannedMeal(1, 1, SavedRecipe.create("MAIN_COURSE", 1, "Dish", "", prep, 0, ""), TODAY,
                MealSlot.DINNER, prep, 0, HealthyLevel.MODERATE);
    }

    @Test
    void theRangeEndsTodayWhenTodayIsPlanned() {
        LocalDate[] range = StreakCalculator.currentRange(Set.of(TODAY, TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
        assertArrayEquals(new LocalDate[] {TODAY.minusDays(2), TODAY}, range);
    }

    @Test
    void theRangeEndsYesterdayWhenTheStreakIsAtRisk() {
        LocalDate[] range = StreakCalculator.currentRange(Set.of(TODAY.minusDays(1), TODAY.minusDays(2)), TODAY);
        assertArrayEquals(new LocalDate[] {TODAY.minusDays(2), TODAY.minusDays(1)}, range);
    }

    @Test
    void noStreakMeansNoRange() {
        assertNull(StreakCalculator.currentRange(Set.of(TODAY.minusDays(3)), TODAY));
        assertNull(StreakCalculator.currentRange(Set.of(), TODAY));
    }

    @Test
    void mealsAreSortedIntoTheThreeTiers() {
        Map<PrepTier, Long> tiers = StreakCalculator.tierCounts(List.of(meal(10), meal(15), meal(30), meal(60)));
        assertEquals(2L, tiers.get(PrepTier.QUICK));
        assertEquals(1L, tiers.get(PrepTier.AVERAGE));
        assertEquals(1L, tiers.get(PrepTier.SLOW));
        assertEquals("2 quick · 1 average · 1 slow", StreakCalculator.tierLine(tiers));
    }

    @Test
    void theBadgeIsNamedAfterTheTierCookedMost() {
        assertEquals("Quick Cook", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(List.of(meal(5), meal(10), meal(60)))));
        assertEquals("Home Cook", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(List.of(meal(30), meal(40)))));
        assertEquals("Slow-Food Chef", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(List.of(meal(90)))));
    }

    @Test
    void aTieGoesToTheQuickerTierAndNoMealsMeansNoBadge() {
        assertEquals("Quick Cook", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(List.of(meal(10), meal(90)))));
        assertEquals("No badge yet", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(List.of())));
        assertEquals("No badge yet", StreakCalculator.badgeTitle(StreakCalculator.tierCounts(null)));
    }
}
