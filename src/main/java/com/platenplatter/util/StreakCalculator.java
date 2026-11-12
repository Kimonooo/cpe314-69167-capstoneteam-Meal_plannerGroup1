package com.platenplatter.util;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * The planning streak: how many days in a row have at least one planned meal, the next
 * streak goal, and the prep-tier badge earned by the meals in the current streak.
 * Nothing here is stored: the streak is worked out again from the planned_meals dates
 * every time, so it is always right after a restart, an import or a removed meal.
 */
public final class StreakCalculator {
    private StreakCalculator() { }

    /** The streak goals, in days. After the last one, the goals go on every 50 days. */
    public static final List<Integer> MILESTONES = List.of(3, 7, 14, 30, 60, 100);
    private static final int LAST_MILESTONE = 100;
    private static final int STEP_AFTER_LAST = 50;

    /**
     * Days in a row with a meal, counting back from today. If today has no meal yet, the streak
     * is still alive and counts back from yesterday.
     */
    public static int current(Set<LocalDate> days, LocalDate today) {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        LocalDate start = days.contains(today) ? today : today.minusDays(1);
        return (int) Stream.iterate(start, day -> day.minusDays(1)).takeWhile(days::contains).count();
    }

    /** The first and last day of the current streak, or null when there is no streak. */
    public static LocalDate[] currentRange(Set<LocalDate> days, LocalDate today) {
        int current = current(days, today);
        if (current == 0) {
            return null;
        }
        LocalDate end = days.contains(today) ? today : today.minusDays(1);
        return new LocalDate[] {end.minusDays(current - 1L), end};
    }

    /** The longest run of days in a row, not counting days after today. */
    public static int longest(Set<LocalDate> days, LocalDate today) {
        return days == null ? 0 : longest(days.stream().filter(day -> !day.isAfter(today)).collect(toSet()));
    }

    /** The longest run of days in a row: from every day that starts a run, count how far it goes. */
    public static int longest(Set<LocalDate> days) {
        if (days == null) {
            return 0;
        }
        return (int) days.stream()
                .filter(day -> !days.contains(day.minusDays(1)))          // a run starts here
                .mapToLong(start -> Stream.iterate(start, day -> day.plusDays(1)).takeWhile(days::contains).count())
                .max()
                .orElse(0);
    }

    /** Yesterday had a meal but today has none yet: plan one today to keep the streak. */
    public static boolean atRisk(Set<LocalDate> days, LocalDate today) {
        return days != null && !days.contains(today) && days.contains(today.minusDays(1));
    }

    /** Monday to Sunday of this week: true for each day that has a meal. */
    public static List<Boolean> weekFlags(Set<LocalDate> days, LocalDate today) {
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        return IntStream.range(0, 7).mapToObj(i -> days != null && days.contains(monday.plusDays(i))).toList();
    }

    /** How many meals of each prep tier (every tier is listed, 0 when it has none). */
    public static Map<PrepTier, Long> tierCounts(List<PlannedMeal> meals) {
        Map<PrepTier, Long> counted = meals == null ? Map.of()
                : meals.stream().collect(groupingBy(PlannedMeal::getPrepTier, counting()));
        return Arrays.stream(PrepTier.values()).collect(toMap(Function.identity(),
                tier -> counted.getOrDefault(tier, 0L), (a, b) -> a, () -> new EnumMap<>(PrepTier.class)));
    }

    /** The tier with the most meals; a tie goes to the QUICKER tier. Null when there are no meals. */
    public static PrepTier mainTier(Map<PrepTier, Long> counts) {
        if (counts == null) {
            return null;
        }
        return Arrays.stream(PrepTier.values())
                .filter(tier -> counts.getOrDefault(tier, 0L) > 0)
                .max(Comparator.comparingLong((PrepTier tier) -> counts.getOrDefault(tier, 0L))
                        .thenComparing(Comparator.comparingInt(PrepTier::ordinal).reversed()))
                .orElse(null);
    }

    /** The next goal above the current streak: 0 -> 3, 3 -> 7, 7 -> 14 ... 100 -> 150, 150 -> 200. */
    public static int nextMilestone(int current) {
        int days = Math.max(current, 0);
        return MILESTONES.stream()
                .filter(goal -> goal > days)
                .findFirst()
                .orElse((days / STEP_AFTER_LAST + 1) * STEP_AFTER_LAST);
    }

    /** The last goal already reached (0 when none yet): 5 -> 3, 7 -> 7, 120 -> 100. */
    public static int reachedMilestone(int current) {
        int days = Math.max(current, 0);
        if (days >= LAST_MILESTONE) {
            return days / STEP_AFTER_LAST * STEP_AFTER_LAST;
        }
        return MILESTONES.stream()
                .filter(goal -> goal <= days)
                .reduce((first, second) -> second)          // the last one that fits
                .orElse(0);
    }

    /** How far the streak is between the goal it reached and the next one, from 0.0 to 1.0. */
    public static double milestoneProgress(int current) {
        int from = reachedMilestone(current);
        int to = nextMilestone(current);
        return (Math.max(current, 0) - from) / (double) (to - from);
    }

    /** "2 more days to reach a 7-day streak" */
    public static String goalLine(int current) {
        int goal = nextMilestone(current);
        int left = goal - Math.max(current, 0);
        return left + (left == 1 ? " more day" : " more days") + " to reach a " + goal + "-day streak";
    }

    /**
     * The badge is named after how long most meals in the streak take to prepare:
     * Speedy Chef (15 min or less), Steady Chef (16 to 45 min), Patient Chef (more than 45 min).
     */
    public static String badgeTitle(Map<PrepTier, Long> counts) {
        PrepTier main = mainTier(counts);
        if (main == null) {
            return "No badge yet";
        }
        return switch (main) {
            case QUICK -> "Speedy Chef";
            case AVERAGE -> "Steady Chef";
            case SLOW -> "Patient Chef";
        };
    }

    /** Why the badge was given: "most meals in this streak take 16 to 45 min". Empty when there is no badge. */
    public static String badgeReason(Map<PrepTier, Long> counts) {
        PrepTier main = mainTier(counts);
        return main == null ? "" : "most meals in this streak take " + main.getRange();
    }

    /** "2 quick · 1 average · 0 slow" */
    public static String tierLine(Map<PrepTier, Long> counts) {
        return Arrays.stream(PrepTier.values())
                .map(tier -> (counts == null ? 0 : counts.getOrDefault(tier, 0L)) + " " + tier.getLabel().toLowerCase())
                .collect(joining(" · "));
    }
}
