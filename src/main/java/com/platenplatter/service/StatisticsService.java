package com.platenplatter.service;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.util.TextFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The DATA MINING step of the Analytics tab. It reads the user's real planned meals
 * and counts them per day and per group, so changing the data changes the chart.
 * Every method uses streams: filter the meals, group them, count them.
 */
public final class StatisticsService {
    private StatisticsService() { }

    /** What the bars of the chart are split by. */
    public enum Grouping {
        HEALTHY_LEVEL("Healthy level"),
        PREP_TIME("Prep time"),
        MEAL_SLOT("Meal slot");

        private final String label;

        Grouping(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        /** The names of the bars, always in the same order (for example Healthy, Moderate, Indulgent). */
        public List<String> seriesNames() {
            return switch (this) {
                case HEALTHY_LEVEL -> Arrays.stream(HealthyLevel.values()).map(HealthyLevel::getLabel).toList();
                case PREP_TIME -> Arrays.stream(PrepTier.values()).map(PrepTier::getLabel).toList();
                case MEAL_SLOT -> Arrays.stream(MealSlot.values()).map(MealSlot::getLabel).toList();
            };
        }

        /** The bar one meal belongs to. */
        public String seriesOf(PlannedMeal meal) {
            return switch (this) {
                case HEALTHY_LEVEL -> meal.getHealthyLevel().getLabel();
                case PREP_TIME -> meal.getPrepTier().getLabel();
                case MEAL_SLOT -> meal.getMealSlot().getLabel();
            };
        }

        /** Every bar name with its count, or 0 when it has none (so empty bars still show). */
        public Map<String, Long> withZeros(Map<String, Long> counts) {
            Map<String, Long> safe = counts == null ? Map.of() : counts;
            return seriesNames().stream().collect(toMap(Function.identity(),
                    name -> safe.getOrDefault(name, 0L), (a, b) -> a, LinkedHashMap::new));
        }

        @Override
        public String toString() {
            return label; // shown in the "Group bars by" box
        }
    }

    public static LocalDate mondayOf(LocalDate day) {
        return day.minusDays(day.getDayOfWeek().getValue() - 1L);
    }

    /** For one week: day -> (group -> number of meals). Meals outside the week are ignored. */
    public static Map<String, Map<String, Long>> countByDay(
            List<PlannedMeal> meals, LocalDate monday, Grouping grouping) {
        LocalDate sunday = monday.plusDays(6);
        Map<LocalDate, Map<String, Long>> counted = meals.stream()
                .filter(m -> !m.getPlanDate().isBefore(monday)
                          && !m.getPlanDate().isAfter(sunday))       // this week only
                .collect(groupingBy(PlannedMeal::getPlanDate,
                         groupingBy(grouping::seriesOf, counting())));
        return IntStream.range(0, 7).mapToObj(monday::plusDays)           // all 7 days
                .collect(toMap(TextFormat::dayLabel,
                        day -> grouping.withZeros(counted.get(day)),  // empty day: 0s
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** For all time: Mon ... Sun -> (group -> number of meals). */
    public static Map<String, Map<String, Long>> countByWeekday(List<PlannedMeal> meals, Grouping grouping) {
        Map<DayOfWeek, Map<String, Long>> counted = meals.stream()
                .collect(groupingBy(m -> m.getPlanDate().getDayOfWeek(),
                         groupingBy(grouping::seriesOf, counting())));
        return Arrays.stream(DayOfWeek.values())
                .collect(toMap(day -> day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        day -> grouping.withZeros(counted.get(day)),
                        (a, b) -> a, LinkedHashMap::new));
    }

    /** Number of meals per group, for the breakdown panel. */
    public static Map<String, Long> totals(List<PlannedMeal> meals, Grouping grouping) {
        return grouping.withZeros(meals.stream().collect(groupingBy(grouping::seriesOf, counting())));
    }

    /** Healthy meals divided by all meals; NaN when there are none (the card shows a dash). */
    public static double healthyShare(List<PlannedMeal> meals) {
        return meals.isEmpty() ? Double.NaN
                : (double) meals.stream().filter(m -> m.getHealthyLevel() == HealthyLevel.HEALTHY).count() / meals.size();
    }

    /** Average calories, only over meals whose calories are known (more than 0). 0 when none are known. */
    public static double averageCalories(List<PlannedMeal> meals) {
        return meals.stream().mapToInt(PlannedMeal::getCalories).filter(c -> c > 0).average().orElse(0);
    }

    public static int totalCalories(List<PlannedMeal> meals) {
        return meals.stream().mapToInt(PlannedMeal::getCalories).sum();
    }

    /** Average prep minutes per meal; 0 when there are no meals. */
    public static double averagePrepMinutes(List<PlannedMeal> meals) {
        return meals.stream().mapToInt(PlannedMeal::getPrepMinutes).average().orElse(0);
    }

    /** Number of meals whose calories are known. */
    public static long withCalories(List<PlannedMeal> meals) {
        return meals.stream().filter(m -> m.getCalories() > 0).count();
    }

    /** The color of each calendar day: the level most of its meals have. */
    public static Map<LocalDate, HealthyLevel> levelByDay(List<PlannedMeal> meals) {
        return meals.stream()
                .collect(groupingBy(PlannedMeal::getPlanDate, TreeMap::new,
                         groupingBy(PlannedMeal::getHealthyLevel, () -> new EnumMap<>(HealthyLevel.class), counting())))
                .entrySet().stream()
                .collect(toMap(Map.Entry::getKey, day -> dominant(day.getValue()), (a, b) -> a, TreeMap::new));
    }

    /** The level with the most meals; a tie goes to the LESS healthy level (null when there are none). */
    public static HealthyLevel dominant(Map<HealthyLevel, Long> counts) {
        return Arrays.stream(HealthyLevel.values())
                .filter(level -> counts.getOrDefault(level, 0L) > 0)
                .max(Comparator.comparingLong((HealthyLevel level) -> counts.getOrDefault(level, 0L))
                        .thenComparingInt(HealthyLevel::ordinal))
                .orElse(null);
    }

    /** The biggest single count in a chart table, for the y-axis. */
    public static long maxCount(Map<String, Map<String, Long>> table) {
        return table.values().stream().flatMap(row -> row.values().stream())
                .mapToLong(Long::longValue).max().orElse(0);
    }
}
