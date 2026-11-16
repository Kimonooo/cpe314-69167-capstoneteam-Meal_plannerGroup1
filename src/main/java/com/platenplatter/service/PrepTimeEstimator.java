package com.platenplatter.util;

import java.util.Locale;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

/**
 * Estimates how long a recipe takes, because TheMealDB gives no prep time.
 *
 *   1. Cooking time: add every "N minutes" or "N hours" written in the steps.
 *      A range such as "1 to 2 minutes" counts its second number; hours count 60 minutes.
 *   2. Handling time: 2 minutes for each ingredient (washing, cutting, measuring).
 *   3. Base prep = step 1 + step 2, kept between 5 and 480 minutes.
 * The SavedRecipe subclass then adds its own extra time (0, 10 or 15 minutes).
 *
 * Spicy Arrabiata Penne: "about 9 minutes" + "1 to 2 minutes" + "for 5 minutes" = 16,
 * plus 8 ingredients x 2 = 16, so the base is 32 minutes (42 minutes as a main course).
 */
public final class PrepTimeEstimator {
    private PrepTimeEstimator() { }

    public static final int MINUTES_PER_INGREDIENT = 2;
    public static final int MIN_MINUTES = 5;
    public static final int MAX_MINUTES = 480;

    private static final Pattern TIME = Pattern.compile(
            "(\\d+)(?:\\s*(?:-|to)\\s*(\\d+))?\\s*(hours?|hrs?|minutes?|mins?)\\b",
            Pattern.CASE_INSENSITIVE);

    public static int estimateBaseMinutes(String instructions, int ingredientCount) {
        int cooking = TIME.matcher(instructions == null ? "" : instructions)
                .results()                                   // every time phrase found
                .mapToInt(PrepTimeEstimator::minutesOf)
                .sum();
        int total = cooking + MINUTES_PER_INGREDIENT * Math.max(ingredientCount, 0);
        return Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, total)); // 5 min to 8 h
    }

    /** Only the cooking minutes written in the steps (step 1), for the hint under the box. */
    public static int cookingMinutes(String instructions) {
        return TIME.matcher(instructions == null ? "" : instructions)
                .results()
                .mapToInt(PrepTimeEstimator::minutesOf)
                .sum();
    }

    private static int minutesOf(MatchResult m) {
        int n = Integer.parseInt(m.group(2) != null ? m.group(2) : m.group(1));
        return m.group(3).toLowerCase(Locale.ROOT).startsWith("h") ? n * 60 : n;
    }
}
