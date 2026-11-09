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
 * The planning streak: how many days in a row have at least one planned meal, and the
 * prep-tier badge earned by the meals in the current streak. Worked out from the dates only.
 */
public final class StreakCalculator {
    private StreakCalculator() { }

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

    public static String badgeTitle(Map<PrepTier, Long> counts) {
        PrepTier main = mainTier(counts);
        if (main == null) {
            return "No badge yet";
        }
        return switch (main) {
            case QUICK -> "Quick Cook";
            case AVERAGE -> "Home Cook";
            case SLOW -> "Slow-Food Chef";
        };
    }

    /** "2 quick · 1 average · 0 slow" */
    public static String tierLine(Map<PrepTier, Long> counts) {
        return Arrays.stream(PrepTier.values())
                .map(tier -> (counts == null ? 0 : counts.getOrDefault(tier, 0L)) + " " + tier.getLabel().toLowerCase())
                .collect(joining(" · "));
    }
}
