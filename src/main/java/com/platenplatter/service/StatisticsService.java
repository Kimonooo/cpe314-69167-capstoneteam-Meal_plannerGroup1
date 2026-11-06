package com.platenplatter.service;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import com.platenplatter.util.MealPlanAnalyzer;
import com.platenplatter.util.PlannedMeal;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * DATA MINING for the Analytics tab. It works on the real rows from the database (never typed-in
 * numbers), so changing a meal changes the result and the chart.
 *
 * The main result is a table of counts: one row per day (Mon to Sun), one column per group, where
 * the group is the meal's Healthy level, Prep time tier, or Meal slot. Every cell starts at 0, so
 * an empty day shows 0 instead of being missing. Uses Maps (Data Structure requirement).
 */
public final class StatisticsService {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);

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

        /** The groups in a fixed order, for example Healthy, Moderate, Indulgent. */
        public List<String> seriesNames() {
            List<String> names = new ArrayList<>();
            switch (this) {
                case HEALTHY_LEVEL:
                    for (HealthyLevel h : HealthyLevel.values()) {
                        names.add(h.getLabel());
                    }
                    break;
                case PREP_TIME:
                    for (PrepTier t : PrepTier.values()) {
                        names.add(t.getLabel());
                    }
                    break;
                default:
                    for (MealSlot s : MealSlot.values()) {
                        names.add(s.getLabel());
                    }
                    break;
            }
            return names;
        }

        /** The group one meal belongs to. */
        public String seriesOf(PlannedMeal meal) {
            switch (this) {
                case HEALTHY_LEVEL:
                    return meal.getHealthyLevel().getLabel();
                case PREP_TIME:
                    return meal.getPrepTier().getLabel();
                default:
                    return meal.getMealSlot().getLabel();
            }
        }

        @Override
        public String toString() {
            return label; // shown in the "Group bars by" box
        }
    }

    /** The Monday of the week a day is in. */
    public static LocalDate mondayOf(LocalDate day) {
        return day.minusDays(day.getDayOfWeek().getValue() - 1L);
    }

    /** For example "Mon 2" (used on the chart's day axis for one week). */
    public static String dayLabel(LocalDate day) {
        return day.format(DAY);
    }

    /**
     * One week, Monday to Sunday: day label -> (group -> number of meals).
     * Meals outside the week are ignored.
     */
    public static Map<String, Map<String, Integer>> countByDay(List<PlannedMeal> rows, LocalDate monday, Grouping grouping) {
        Map<String, Map<String, Integer>> table = new LinkedHashMap<>();
        for (int d = 0; d < 7; d++) {
            table.put(dayLabel(monday.plusDays(d)), zeros(grouping));
        }
        LocalDate sunday = monday.plusDays(6);
        for (PlannedMeal meal : safe(rows)) {
            LocalDate date = dateOf(meal);
            if (date != null && !date.isBefore(monday) && !date.isAfter(sunday)) {
                table.get(dayLabel(date)).merge(grouping.seriesOf(meal), 1, Integer::sum);
            }
        }
        return table;
    }

    /** Every meal, by day of the week: "Mon" ... "Sun" -> (group -> number of meals). */
    public static Map<String, Map<String, Integer>> countByWeekday(List<PlannedMeal> rows, Grouping grouping) {
        Map<String, Map<String, Integer>> table = new LinkedHashMap<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            table.put(weekdayLabel(day), zeros(grouping));
        }
        for (PlannedMeal meal : safe(rows)) {
            LocalDate date = dateOf(meal);
            if (date != null) {
                table.get(weekdayLabel(date.getDayOfWeek())).merge(grouping.seriesOf(meal), 1, Integer::sum);
            }
        }
        return table;
    }

    /** Group -> number of meals, every group listed (0 when it has no meals). */
    public static Map<String, Integer> totals(List<PlannedMeal> rows, Grouping grouping) {
        Map<String, Integer> totals = zeros(grouping);
        for (PlannedMeal meal : safe(rows)) {
            totals.merge(grouping.seriesOf(meal), 1, Integer::sum);
        }
        return totals;
    }

    /** The design-document version: date -> (Healthy level -> number of meals) for one week. */
    public static Map<LocalDate, EnumMap<HealthyLevel, Integer>> countByDayAndLevel(List<PlannedMeal> rows, LocalDate monday) {
        Map<LocalDate, EnumMap<HealthyLevel, Integer>> counts = new TreeMap<>();
        for (int d = 0; d < 7; d++) {
            EnumMap<HealthyLevel, Integer> day = new EnumMap<>(HealthyLevel.class);
            for (HealthyLevel h : HealthyLevel.values()) {
                day.put(h, 0);
            }
            counts.put(monday.plusDays(d), day);
        }
        for (PlannedMeal meal : safe(rows)) {
            LocalDate date = dateOf(meal);
            EnumMap<HealthyLevel, Integer> day = date == null ? null : counts.get(date); // TreeMap.get(null) throws
            if (day != null) {
                day.merge(meal.getHealthyLevel(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** Healthy meals divided by all meals (0.0 to 1.0), or NaN when there are no meals. */
    public static double healthyShare(List<PlannedMeal> rows) {
        List<PlannedMeal> list = safe(rows);
        if (list.isEmpty()) {
            return Double.NaN;
        }
        int healthy = 0;
        for (PlannedMeal meal : list) {
            if (meal.getHealthyLevel() == HealthyLevel.HEALTHY) {
                healthy++;
            }
        }
        return (double) healthy / list.size();
    }

    /** Average calories of the meals whose calories are known (more than 0); 0 when none are known. */
    public static double averageCalories(List<PlannedMeal> rows) {
        long sum = 0;
        int n = 0;
        for (PlannedMeal meal : safe(rows)) {
            if (meal.getCalories() > 0) {
                sum += meal.getCalories();
                n++;
            }
        }
        return n == 0 ? 0 : (double) sum / n;
    }

    /** Total calories of the meals whose calories are known. */
    public static int totalCalories(List<PlannedMeal> rows) {
        int sum = 0;
        for (PlannedMeal meal : safe(rows)) {
            sum += Math.max(meal.getCalories(), 0);
        }
        return sum;
    }

    public static double averagePrepMinutes(List<PlannedMeal> rows) {
        List<Integer> minutes = new ArrayList<>();
        for (PlannedMeal meal : safe(rows)) {
            minutes.add(meal.getPrepMinutes());
        }
        return MealPlanAnalyzer.averagePrepMinutes(minutes);
    }

    /**
     * The color of each day on the calendar: the level most of that day's meals have.
     * A tie goes to the LESS healthy level, so a day is never shown greener than it is.
     */
    public static Map<LocalDate, HealthyLevel> levelByDay(List<PlannedMeal> rows) {
        Map<LocalDate, EnumMap<HealthyLevel, Integer>> perDay = new TreeMap<>();
        for (PlannedMeal meal : safe(rows)) {
            LocalDate date = dateOf(meal);
            if (date != null) {
                perDay.computeIfAbsent(date, d -> new EnumMap<>(HealthyLevel.class))
                        .merge(meal.getHealthyLevel(), 1, Integer::sum);
            }
        }
        Map<LocalDate, HealthyLevel> out = new TreeMap<>();
        for (Map.Entry<LocalDate, EnumMap<HealthyLevel, Integer>> e : perDay.entrySet()) {
            out.put(e.getKey(), dominant(e.getValue()));
        }
        return out;
    }

    /** The level with the most meals; a tie goes to the less healthy one. Null when empty. */
    public static HealthyLevel dominant(Map<HealthyLevel, Integer> counts) {
        HealthyLevel best = null;
        int bestCount = 0;
        for (HealthyLevel level : HealthyLevel.values()) { // Healthy, Moderate, Indulgent
            int n = counts.getOrDefault(level, 0);
            if (n > 0 && n >= bestCount) { // ">=" lets a later (less healthy) level win a tie
                best = level;
                bestCount = n;
            }
        }
        return best;
    }

    /** The biggest number in a counts table (used to size the chart's side scale). */
    public static int maxCount(Map<String, Map<String, Integer>> table) {
        int max = 0;
        for (Map<String, Integer> row : table.values()) {
            for (int n : row.values()) {
                max = Math.max(max, n);
            }
        }
        return max;
    }

    private static String weekdayLabel(DayOfWeek day) {
        return day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private static Map<String, Integer> zeros(Grouping grouping) {
        Map<String, Integer> row = new LinkedHashMap<>();
        for (String name : grouping.seriesNames()) {
            row.put(name, 0);
        }
        return row;
    }

    private static LocalDate dateOf(PlannedMeal meal) {
        try {
            return meal.getLocalDate();
        } catch (DateTimeParseException e) {
            return null; // a badly written date is skipped, never a crash
        }
    }

    private static List<PlannedMeal> safe(List<PlannedMeal> rows) {
        return rows == null ? new ArrayList<>() : rows;
    }
}
