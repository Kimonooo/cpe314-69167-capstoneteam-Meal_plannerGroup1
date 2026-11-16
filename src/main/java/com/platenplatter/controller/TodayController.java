package com.platenplatter.controller;

import com.platenplatter.db.PlanStore;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StreakCalculator;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The controller of the Today page and of the streak pills in the banner. It reads the planned
 * days once and works out everything both show; the streak is never stored, it is counted from
 * planned_meals every time (StreakCalculator). Call the reading methods from a background Task.
 */
public class TodayController {

    /** The streak as the pages show it. */
    public record StreakView(int current, int best, boolean atRisk, String badgeTitle,
                             String badgeReason, String tierLine, double goalProgress,
                                     String goalLine,
                             List<Boolean> weekFlags, LocalDate monday) {
        public boolean hasBadge() {
            return current > 0;
        }
    }

    /** Everything on the Today page: the streak, and today's meals by meal slot. */
    public record TodayView(StreakView streak, List<PlannedMeal> meals, int calories,
                            Map<MealSlot, List<PlannedMeal>> bySlot) { }

    private final PlanStore store;

    public TodayController(PlanStore store) {
        this.store = store;
    }

    /** The streak on that day (the banner pills). */
    public StreakView streak(LocalDate today) throws SQLException {
        return streak(store.plannedDates(), today);
    }

    /** The whole Today page. */
    public TodayView today(LocalDate today) throws SQLException {
        Set<LocalDate> days = store.plannedDates();
        List<PlannedMeal> meals = store.mealsOn(today);
        Map<MealSlot, List<PlannedMeal>> bySlot = new EnumMap<>(MealSlot.class);
        for (MealSlot slot : MealSlot.values()) {
            bySlot.put(slot, meals.stream().filter(m -> m.getMealSlot() == slot).toList());
        }
        return new TodayView(streak(days, today), meals, StatisticsService.totalCalories(meals),
                bySlot);
    }

    private StreakView streak(Set<LocalDate> days, LocalDate today) throws SQLException {
        LocalDate[] range = StreakCalculator.currentRange(days, today);
        List<PlannedMeal> streakMeals = range == null ? List.of() : store.mealsBetween(range[0],
                range[1]);
        Map<PrepTier, Long> tiers = StreakCalculator.tierCounts(streakMeals);
        int current = StreakCalculator.current(days, today);
        return new StreakView(current, StreakCalculator.longest(days, today),
                StreakCalculator.atRisk(days, today), StreakCalculator.badgeTitle(tiers),
                StreakCalculator.badgeReason(tiers), StreakCalculator.tierLine(tiers),
                StreakCalculator.milestoneProgress(current), StreakCalculator.goalLine(current),
                StreakCalculator.weekFlags(days, today), StatisticsService.mondayOf(today));
    }
}
