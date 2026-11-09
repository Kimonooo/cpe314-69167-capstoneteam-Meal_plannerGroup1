package com.platenplatter.util;

import com.platenplatter.model.PrepTier;

import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The STREAK: how many days in a row have at least one planned meal.
 * Rule: the streak counts back from today. If nothing is planned today YET but yesterday had a meal,
 * the streak is still alive ("at risk"): plan something today to keep it. If neither today nor
 * yesterday has a meal, the streak is 0.
 *
 * The streak BADGE also sorts the meals of the current streak by prep time (Quick, Average, Slow),
 * and names the badge after the tier the user cooks most: Quick Cook, Home Cook or Slow-Food Chef.
 */
public final class StreakCalculator {
    private StreakCalculator() { }

    /**
     * The first and last day of the current streak, or null when there is no streak.
     * The last day is today, or yesterday when the streak is "at risk".
     */
    public static LocalDate[] currentRange(Set<LocalDate> days, LocalDate today) {
        int current = current(days, today);
        if (current == 0) {
            return null;
        }
        LocalDate end = days.contains(today) ? today : today.minusDays(1);
        return new LocalDate[] {end.minusDays(current - 1L), end};
    }

    /** How many meals are Quick, Average and Slow. Every tier is listed, even with 0. */
    public static EnumMap<PrepTier, Integer> tierCounts(List<PlannedMeal> meals) {
        EnumMap<PrepTier, Integer> counts = new EnumMap<>(PrepTier.class);
        for (PrepTier tier : PrepTier.values()) {
            counts.put(tier, 0);
        }
        if (meals != null) {
            for (PlannedMeal meal : meals) {
                counts.merge(meal.getPrepTier(), 1, Integer::sum);
            }
        }
        return counts;
    }

    /** The tier with the most meals (a tie goes to the quicker tier), or null when there are none. */
    public static PrepTier mainTier(Map<PrepTier, Integer> counts) {
        PrepTier best = null;
        int bestCount = 0;
        for (PrepTier tier : PrepTier.values()) { // Quick, Average, Slow
            int n = counts == null ? 0 : counts.getOrDefault(tier, 0);
            if (n > bestCount) {
                best = tier;
                bestCount = n;
            }
        }
        return best;
    }

    /** The badge name: Quick Cook, Home Cook or Slow-Food Chef, or "No badge yet". */
    public static String badgeTitle(Map<PrepTier, Integer> counts) {
        PrepTier main = mainTier(counts);
        if (main == null) {
            return "No badge yet";
        }
        switch (main) {
            case QUICK: return "Quick Cook";
            case AVERAGE: return "Home Cook";
            default: return "Slow-Food Chef";
        }
    }

    /** For example "3 quick · 2 average · 1 slow". */
    public static String tierLine(Map<PrepTier, Integer> counts) {
        StringBuilder line = new StringBuilder();
        for (PrepTier tier : PrepTier.values()) {
            if (line.length() > 0) {
                line.append(" · ");
            }
            int n = counts == null ? 0 : counts.getOrDefault(tier, 0);
            line.append(n).append(' ').append(tier.getLabel().toLowerCase());
        }
        return line.toString();
    }

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
