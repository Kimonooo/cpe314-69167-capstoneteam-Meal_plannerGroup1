package com.platenplatter.controller;

import com.platenplatter.db.PlanStore;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.TextFormat;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The controller of the Analytics page. It reads the user's meals (one week, or all of them),
 * lets StatisticsService mine them (the DATA MINING), and returns everything the page draws:
 * the counts for the chart, its title and axis label, the four summary numbers and the
 * breakdown per group. Call load from a background Task.
 */
public class AnalyticsController {
    /** Everything the page shows, worked out on the background thread. */
    public record Snapshot(boolean weekScope, LocalDate monday, Grouping grouping,
                           Map<String, Map<String, Long>> table, List<PlannedMeal> meals,
                           String chartTitle, String xAxisLabel, double healthyShare,
                           double averagePrepMinutes, double averageCalories, long withCalories,
                           Map<Grouping, Map<String, Long>> breakdown) {
        public boolean isEmpty() {
            return meals.isEmpty();
        }
    }

    private final PlanStore store;

    public AnalyticsController(PlanStore store) {
        this.store = store;
    }

    /**
     * One week (Monday to Sunday) counted per day, or all time counted per weekday. In the week
     * view the bars are the seven dates; in the all-time view they are Mon to Sun, so the axis
     * says "Weekday" and the title says "per Weekday".
     */
    public Snapshot load(boolean weekScope, LocalDate monday,
            Grouping grouping) throws SQLException {
        List<PlannedMeal> meals = weekScope ? store.mealsBetween(monday, monday.plusDays(6))
                : store.allMeals();
        Map<String, Map<String, Long>> table = weekScope
                ? StatisticsService.countByDay(meals, monday, grouping)
                : StatisticsService.countByWeekday(meals, grouping);
        Map<Grouping, Map<String, Long>> breakdown = new LinkedHashMap<>();
        Arrays.stream(Grouping.values())
                .forEach(g -> breakdown.put(g, StatisticsService.totals(meals, g)));
        String per = weekScope ? "Day" : "Weekday";
        String title = "Meals Planned per " + per + " by " + titleCase(grouping.getLabel())
                + (weekScope ? ", Week of " + weekLabel(monday) : ", All Time");
        return new Snapshot(weekScope, monday, grouping, table, meals, title, per,
                StatisticsService.healthyShare(meals), StatisticsService.averagePrepMinutes(meals),
                StatisticsService.averageCalories(meals), StatisticsService.withCalories(meals),
                        breakdown);
    }

    /** "Mon, 9 Nov 2026": the Monday of the week, in the app's one date format. */
    public static String weekLabel(LocalDate monday) {
        return TextFormat.shortDate(monday);
    }

    /** The Monday of the week a day is in. */
    public static LocalDate mondayOf(LocalDate day) {
        return StatisticsService.mondayOf(day);
    }

    /** "Healthy level" -> "Healthy Level" */
    static String titleCase(String text) {
        return Arrays.stream(text.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
