package com.platenplatter.util;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * DATA MINING step: frequency analysis over the planned meals.
 * Input comes from the database (never hard-coded), so changing the data
 * changes the result AND the chart. Uses a Map (Data Structure requirement).
 */
public final class MealPlanAnalyzer {
    private MealPlanAnalyzer() { }

    /** Counts how many planned meals fall in each category. Blank/null = "Uncategorized". */
    public static Map<String, Integer> countByCategory(List<String> categories) {
        Map<String, Integer> counts = new TreeMap<>();
        if (categories == null) {
            return counts;
        }
        for (String c : categories) {
            String key = (c == null || c.isBlank()) ? "Uncategorized" : c.trim();
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    /** Category with the highest count; ties go to the alphabetically first. Null if empty. */
    public static String mostFrequent(Map<String, Integer> counts) {
        String best = null;
        int bestCount = 0;
        if (counts == null) {
            return null;
        }
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (e.getValue() > bestCount) {
                best = e.getKey();
                bestCount = e.getValue();
            }
        }
        return best;
    }

    /** Average prep minutes, ignoring null/negative values. Returns 0 for no valid data. */
    public static double averagePrepMinutes(List<Integer> minutes) {
        if (minutes == null) {
            return 0;
        }
        int sum = 0;
        int n = 0;
        for (Integer m : minutes) {
            if (m != null && m >= 0) {
                sum += m;
                n++;
            }
        }
        return n == 0 ? 0 : (double) sum / n;
    }
}
