package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.service.StatisticsService.Grouping;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class StatisticsServiceTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private static PlannedMeal meal(LocalDate date, HealthyLevel level, int prep, MealSlot slot, int calories) {
        return new PlannedMeal(1, 1, SavedRecipe.create("MAIN_COURSE", 1, "Dish", "", prep, 0, ""), date, slot, prep, calories, level);
    }

    private static PlannedMeal meal(LocalDate date, HealthyLevel level) {
        return meal(date, level, 30, MealSlot.DINNER, 0);
    }

    @Test
    void mondayOfAnyDayInTheWeekIsTheSameMonday() {
        assertEquals(MONDAY, StatisticsService.mondayOf(LocalDate.of(2026, 11, 5)));
        assertEquals(MONDAY, StatisticsService.mondayOf(LocalDate.of(2026, 11, 8)));
        assertEquals(MONDAY, StatisticsService.mondayOf(MONDAY));
    }

    @Test
    void anEmptyWeekHasSevenDaysOfZeros() {
        Map<String, Map<String, Long>> table = StatisticsService.countByDay(List.of(), MONDAY, Grouping.HEALTHY_LEVEL);
        assertEquals(List.of("Mon 2", "Tue 3", "Wed 4", "Thu 5", "Fri 6", "Sat 7", "Sun 8"), new ArrayList<>(table.keySet()));
        table.values().forEach(day -> {
            assertEquals(List.of("Healthy", "Moderate", "Indulgent"), new ArrayList<>(day.keySet()));
            assertTrue(day.values().stream().allMatch(n -> n == 0));
        });
        assertEquals(0, StatisticsService.maxCount(table));
    }

    @Test
    void mealsAreCountedOnTheirDayAndLevelAndOtherWeeksAreIgnored() {
        List<PlannedMeal> meals = List.of(meal(MONDAY, HealthyLevel.HEALTHY), meal(MONDAY, HealthyLevel.HEALTHY),
                meal(MONDAY, HealthyLevel.INDULGENT), meal(MONDAY.minusDays(1), HealthyLevel.INDULGENT),
                meal(MONDAY.plusDays(7), HealthyLevel.HEALTHY));
        Map<String, Map<String, Long>> table = StatisticsService.countByDay(meals, MONDAY, Grouping.HEALTHY_LEVEL);
        assertEquals(2L, table.get("Mon 2").get("Healthy"));
        assertEquals(0L, table.get("Mon 2").get("Moderate"));
        assertEquals(1L, table.get("Mon 2").get("Indulgent"));
        assertEquals(0L, table.get("Sun 8").get("Indulgent"));
        assertEquals(2, StatisticsService.maxCount(table));
    }

    @Test
    void theSameMealsCanBeGroupedByPrepTimeOrMealSlot() {
        List<PlannedMeal> meals = List.of(meal(MONDAY, HealthyLevel.HEALTHY, 10, MealSlot.BREAKFAST, 0),
                meal(MONDAY, HealthyLevel.HEALTHY, 30, MealSlot.DINNER, 0),
                meal(MONDAY, HealthyLevel.HEALTHY, 60, MealSlot.DINNER, 0));
        assertEquals(Map.of("Quick", 1L, "Average", 1L, "Slow", 1L), StatisticsService.totals(meals, Grouping.PREP_TIME));
        Map<String, Long> slots = StatisticsService.totals(meals, Grouping.MEAL_SLOT);
        assertEquals(List.of("Breakfast", "Lunch", "Dinner", "Snack"), new ArrayList<>(slots.keySet()));
        assertEquals(1L, slots.get("Breakfast"));
        assertEquals(0L, slots.get("Lunch"));
        assertEquals(2L, slots.get("Dinner"));
    }

    @Test
    void allTimeAddsUpEveryMealByDayOfTheWeek() {
        List<PlannedMeal> meals = List.of(meal(MONDAY, HealthyLevel.MODERATE), meal(MONDAY.plusWeeks(3), HealthyLevel.MODERATE),
                meal(MONDAY.plusDays(6), HealthyLevel.HEALTHY));
        Map<String, Map<String, Long>> table = StatisticsService.countByWeekday(meals, Grouping.HEALTHY_LEVEL);
        assertEquals(List.of("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"), new ArrayList<>(table.keySet()));
        assertEquals(2L, table.get("Mon").get("Moderate"));
        assertEquals(1L, table.get("Sun").get("Healthy"));
    }

    @Test
    void healthyShareAndAveragesUseOnlyWhatIsKnown() {
        List<PlannedMeal> meals = List.of(meal(MONDAY, HealthyLevel.HEALTHY, 20, MealSlot.LUNCH, 500),
                meal(MONDAY, HealthyLevel.MODERATE, 30, MealSlot.DINNER, 700));
        assertEquals(0.5, StatisticsService.healthyShare(meals), 0.0001);
        assertEquals(600.0, StatisticsService.averageCalories(meals), 0.0001);  // (500 + 700) / 2
        assertEquals(1200, StatisticsService.totalCalories(meals));
        assertEquals(25.0, StatisticsService.averagePrepMinutes(meals), 0.0001);
        assertEquals(2, StatisticsService.withCalories(meals));
    }

    @Test
    void noMealsGiveNoShareAndNoAverageNotACrash() {
        assertTrue(Double.isNaN(StatisticsService.healthyShare(List.of())));
        assertEquals(0.0, StatisticsService.averageCalories(List.of()), 0.0001);
        assertEquals(0.0, StatisticsService.averagePrepMinutes(List.of()), 0.0001);
    }

    @Test
    void unknownCaloriesAreLeftOutOfTheAverage() {
        List<PlannedMeal> meals = List.of(meal(MONDAY, HealthyLevel.HEALTHY, 20, MealSlot.LUNCH, 500),
                meal(MONDAY, HealthyLevel.HEALTHY, 20, MealSlot.LUNCH, 0));
        assertEquals(500.0, StatisticsService.averageCalories(meals), 0.0001);
        assertEquals(1, StatisticsService.withCalories(meals));
    }

    @Test
    void aDayIsColoredByMostOfItsMealsAndATieShowsTheLessHealthyColor() {
        Map<LocalDate, HealthyLevel> levels = StatisticsService.levelByDay(List.of(
                meal(MONDAY, HealthyLevel.HEALTHY), meal(MONDAY, HealthyLevel.HEALTHY), meal(MONDAY, HealthyLevel.MODERATE),
                meal(MONDAY.plusDays(1), HealthyLevel.HEALTHY), meal(MONDAY.plusDays(1), HealthyLevel.MODERATE)));
        assertEquals(HealthyLevel.HEALTHY, levels.get(MONDAY));
        assertEquals(HealthyLevel.MODERATE, levels.get(MONDAY.plusDays(1)));
        assertNull(levels.get(MONDAY.plusDays(2)));
        assertNull(StatisticsService.dominant(Map.of()));
    }

    @Test
    void changingAMealsLevelChangesTheMinedResult() {
        List<PlannedMeal> before = List.of(meal(MONDAY.plusDays(2), HealthyLevel.MODERATE));
        assertEquals(1L, StatisticsService.countByDay(before, MONDAY, Grouping.HEALTHY_LEVEL).get("Wed 4").get("Moderate"));
        List<PlannedMeal> after = List.of(meal(MONDAY.plusDays(2), HealthyLevel.HEALTHY));
        Map<String, Long> wednesday = StatisticsService.countByDay(after, MONDAY, Grouping.HEALTHY_LEVEL).get("Wed 4");
        assertEquals(0L, wednesday.get("Moderate"));
        assertEquals(1L, wednesday.get("Healthy"));
    }
}
