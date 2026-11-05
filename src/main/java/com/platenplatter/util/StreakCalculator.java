package com.platenplatter.util;

import java.time.LocalDate;
import java.util.Set;
import java.util.TreeSet;

/**
 * The STREAK: how many days in a row have at least one planned meal.
 * Rule: the streak counts back from today. If nothing is planned today YET but yesterday had a meal,
 * the streak is still alive ("at risk"): plan something today to keep it. If neither today nor
 * yesterday has a meal, the streak is 0.
 */
public final class StreakCalculator {
    private StreakCalculator() { }

    /** The current streak in days. */
    public static int current(Set<LocalDate> days, LocalDate today) {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        LocalDate cursor = days.contains(today) ? today : today.minusDays(1);
        int count = 0;
        while (days.contains(cursor)) {
            count++;
            cursor = cursor.minusDays(1);
        }
        return count;
    }

    /** The best streak up to today: days that have not happened yet (planned ahead) do not count. */
    public static int longest(Set<LocalDate> days, LocalDate today) {
        if (days == null) {
            return 0;
        }
        Set<LocalDate> upToToday = new TreeSet<>();
        for (LocalDate day : days) {
            if (!day.isAfter(today)) {
                upToToday.add(day);
            }
        }
        return longest(upToToday);
    }

    /** The longest run of days in a row anywhere in the set. */
    public static int longest(Set<LocalDate> days) {
        if (days == null || days.isEmpty()) {
            return 0;
        }
        int best = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate day : new TreeSet<>(days)) {
            run = (previous != null && previous.plusDays(1).equals(day)) ? run + 1 : 1;
            best = Math.max(best, run);
            previous = day;
        }
        return best;
    }

    /** True when the streak is alive but today still has no meal. */
    public static boolean atRisk(Set<LocalDate> days, LocalDate today) {
        return days != null && !days.contains(today) && days.contains(today.minusDays(1));
    }

    /** Seven true/false values for Monday to Sunday of today's week: is a meal planned that day? */
    public static boolean[] weekFlags(Set<LocalDate> days, LocalDate today) {
        boolean[] flags = new boolean[7];
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 7; i++) {
            flags[i] = days != null && days.contains(monday.plusDays(i));
        }
        return flags;
    }
}
