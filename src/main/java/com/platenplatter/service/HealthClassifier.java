package com.platenplatter.util;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;

/**
 * Decides if a meal is Healthy, Moderate or Indulgent.
 *
 * When the calories are known (more than 0), the meal is compared with its slot's share of a
 * 2,000-calorie day (see MealSlot):
 *   Healthy   = at or under the budget           (lunch: up to 650 kcal)
 *   Moderate  = up to one and a half times it    (lunch: 651 to 975 kcal)
 *   Indulgent = more than that                   (lunch: over 975 kcal)
 *
 * When the calories are unknown (0), TheMealDB category is used instead, the same rule as the
 * design document: Vegetarian, Vegan and Seafood are Healthy, Dessert is Indulgent, the rest Moderate.
 * The user can always pick a different level on the Recipes tab; this is only the automatic choice.
 */
public final class HealthClassifier {
    private HealthClassifier() { }

    /** Moderate meals can go up to this many times the slot's budget. */
    public static final double MODERATE_FACTOR = 1.5;

    public static HealthyLevel classify(int calories, MealSlot slot, String category) {
        if (calories < 0) {
            throw new IllegalArgumentException("Calories cannot be negative: " + calories);
        }
        MealSlot s = slot == null ? MealSlot.defaultFor(category) : slot;
        if (calories > 0) {
            int budget = s.getCalorieBudget();
            if (calories <= budget) {
                return HealthyLevel.HEALTHY;
            }
            if (calories <= Math.round(budget * MODERATE_FACTOR)) {
                return HealthyLevel.MODERATE;
            }
            return HealthyLevel.INDULGENT;
        }
        return byCategory(category);
    }

    /** The fallback rule when the calories are not known. */
    public static HealthyLevel byCategory(String category) {
        if (category == null) {
            return HealthyLevel.MODERATE;
        }
        String c = category.trim();
        if (c.equalsIgnoreCase("Vegetarian") || c.equalsIgnoreCase("Vegan") || c.equalsIgnoreCase("Seafood")) {
            return HealthyLevel.HEALTHY;
        }
        if (c.equalsIgnoreCase("Dessert")) {
            return HealthyLevel.INDULGENT;
        }
        return HealthyLevel.MODERATE;
    }

    /** One line that explains the automatic choice, for example "650 kcal budget for Lunch". */
    public static String explain(int calories, MealSlot slot, String category) {
        MealSlot s = slot == null ? MealSlot.defaultFor(category) : slot;
        if (calories > 0) {
            return calories + " kcal vs " + s.getCalorieBudget() + " kcal budget for " + s.getLabel();
        }
        return "no calories given, so it follows the recipe type";
    }
}
