package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.PlannedMeal;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);
    private long nextId = 1;

    private PlannedMeal meal(String date, MealSlot slot, int prep, int calories, HealthyLevel level) {
        return new PlannedMeal(nextId++, date, "Dish " + nextId, "MainCourse", prep, slot, calories, level);
    }

    @Test
    void mondayOfAnyDayInTheWeekIsTheSameMonday() {
        assertEquals(MONDAY, StatisticsService.mondayOf(LocalDate.of(2026, 11, 5)));
        assertEquals(MONDAY, StatisticsService.mondayOf(LocalDate.of(2026, 11, 8)));
        assertEquals(MONDAY, StatisticsService.mondayOf(MONDAY));
    }

    @Test
    void anEmptyWeekHasSevenDaysOfZeros() {
        Map<String, Map<String, Integer>> table = StatisticsService.countByDay(new ArrayList<>(), MONDAY, Grouping.HEALTHY_LEVEL);
        assertEquals(7, table.size());
        assertEquals(List.of("Mon 2", "Tue 3", "Wed 4", "Thu 5", "Fri 6", "Sat 7", "Sun 8"), new ArrayList<>(table.keySet()));
        for (Map<String, Integer> day : table.values()) {
            assertEquals(List.of("Healthy", "Moderate", "Indulgent"), new ArrayList<>(day.keySet()));
            assertTrue(day.values().stream().allMatch(n -> n == 0));
        }
        assertEquals(0, StatisticsService.maxCount(table));
    }

    @Test
    void mealsAreCountedOnTheirDayAndLevelAndOtherWeeksAreIgnored() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-02", MealSlot.BREAKFAST, 10, 300, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.LUNCH, 30, 600, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.DINNER, 60, 1200, HealthyLevel.INDULGENT),
                meal("2026-11-09", MealSlot.DINNER, 60, 1200, HealthyLevel.INDULGENT)); // next week
        Map<String, Map<String, Integer>> table = StatisticsService.countByDay(rows, MONDAY, Grouping.HEALTHY_LEVEL);
        assertEquals(2, table.get("Mon 2").get("Healthy"));
        assertEquals(0, table.get("Mon 2").get("Moderate"));
        assertEquals(1, table.get("Mon 2").get("Indulgent"));
        assertEquals(0, table.get("Sun 8").get("Indulgent"));
        assertEquals(2, StatisticsService.maxCount(table));
    }

    @Test
    void theSameMealsCanBeGroupedByPrepTimeOrMealSlot() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-03", MealSlot.BREAKFAST, 10, 0, null),
                meal("2026-11-03", MealSlot.DINNER, 30, 0, null),
                meal("2026-11-03", MealSlot.DINNER, 90, 0, null));
        Map<String, Integer> tiers = StatisticsService.countByDay(rows, MONDAY, Grouping.PREP_TIME).get("Tue 3");
        assertEquals(Map.of("Quick", 1, "Average", 1, "Slow", 1), tiers);
        Map<String, Integer> slots = StatisticsService.totals(rows, Grouping.MEAL_SLOT);
        assertEquals(List.of("Breakfast", "Lunch", "Dinner", "Snack"), new ArrayList<>(slots.keySet()));
        assertEquals(1, slots.get("Breakfast"));
        assertEquals(0, slots.get("Lunch"));
        assertEquals(2, slots.get("Dinner"));
    }

    @Test
    void allTimeAddsUpEveryMealByDayOfTheWeek() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-02", MealSlot.LUNCH, 20, 0, HealthyLevel.MODERATE),  // a Monday
                meal("2026-10-26", MealSlot.LUNCH, 20, 0, HealthyLevel.MODERATE),  // another Monday
                meal("2026-11-08", MealSlot.LUNCH, 20, 0, HealthyLevel.HEALTHY));  // a Sunday
        Map<String, Map<String, Integer>> table = StatisticsService.countByWeekday(rows, Grouping.HEALTHY_LEVEL);
        assertEquals(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), new ArrayList<>(table.keySet()));
        assertEquals(2, table.get("Mon").get("Moderate"));
        assertEquals(1, table.get("Sun").get("Healthy"));
    }

    @Test
    void theDesignDocumentMethodGivesTheSameCountsKeyedByDate() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-02", MealSlot.LUNCH, 20, 0, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.SNACK, 5, 0, HealthyLevel.INDULGENT),
                meal("not-a-date", MealSlot.SNACK, 5, 0, HealthyLevel.INDULGENT)); // skipped, never a crash
        Map<LocalDate, EnumMap<HealthyLevel, Integer>> counts = StatisticsService.countByDayAndLevel(rows, MONDAY);
        assertEquals(7, counts.size());
        assertEquals(1, counts.get(MONDAY).get(HealthyLevel.HEALTHY));
        assertEquals(1, counts.get(MONDAY).get(HealthyLevel.INDULGENT));
        assertEquals(0, counts.get(MONDAY.plusDays(1)).get(HealthyLevel.HEALTHY));
    }

    @Test
    void healthyShareAndAveragesUseOnlyWhatIsKnown() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-02", MealSlot.LUNCH, 10, 500, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.DINNER, 30, 700, HealthyLevel.MODERATE),
                meal("2026-11-03", MealSlot.SNACK, 20, 0, HealthyLevel.HEALTHY),     // calories not known
                meal("2026-11-03", MealSlot.DINNER, 40, 0, HealthyLevel.INDULGENT));
        assertEquals(0.5, StatisticsService.healthyShare(rows), 0.0001);
        assertEquals(600.0, StatisticsService.averageCalories(rows), 0.0001);  // (500 + 700) / 2
        assertEquals(1200, StatisticsService.totalCalories(rows));
        assertEquals(25.0, StatisticsService.averagePrepMinutes(rows), 0.0001);
    }

    @Test
    void noMealsGiveNoShareAndNoAverageNotACrash() {
        assertTrue(Double.isNaN(StatisticsService.healthyShare(new ArrayList<>())));
        assertEquals(0.0, StatisticsService.averageCalories(null), 0.0001);
        assertEquals(0.0, StatisticsService.averagePrepMinutes(new ArrayList<>()), 0.0001);
    }

    @Test
    void aDayIsColoredByMostOfItsMealsAndATieShowsTheLessHealthyColor() {
        List<PlannedMeal> rows = List.of(
                meal("2026-11-02", MealSlot.BREAKFAST, 10, 0, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.LUNCH, 10, 0, HealthyLevel.HEALTHY),
                meal("2026-11-02", MealSlot.DINNER, 10, 0, HealthyLevel.INDULGENT),
                meal("2026-11-03", MealSlot.LUNCH, 10, 0, HealthyLevel.HEALTHY),
                meal("2026-11-03", MealSlot.DINNER, 10, 0, HealthyLevel.MODERATE));
        Map<LocalDate, HealthyLevel> levels = StatisticsService.levelByDay(rows);
        assertEquals(HealthyLevel.HEALTHY, levels.get(LocalDate.of(2026, 11, 2)));
        assertEquals(HealthyLevel.MODERATE, levels.get(LocalDate.of(2026, 11, 3)));
        assertNull(levels.get(LocalDate.of(2026, 11, 4)));
    }

    @Test
    void changingAMealsLevelChangesTheMinedResult() {
        List<PlannedMeal> rows = new ArrayList<>();
        rows.add(meal("2026-11-04", MealSlot.DINNER, 30, 900, HealthyLevel.MODERATE));
        assertEquals(1, StatisticsService.countByDay(rows, MONDAY, Grouping.HEALTHY_LEVEL).get("Wed 4").get("Moderate"));
        rows.set(0, meal("2026-11-04", MealSlot.DINNER, 30, 600, HealthyLevel.HEALTHY));
        Map<String, Integer> wednesday = StatisticsService.countByDay(rows, MONDAY, Grouping.HEALTHY_LEVEL).get("Wed 4");
        assertEquals(0, wednesday.get("Moderate"));
        assertEquals(1, wednesday.get("Healthy"));
    }
}
