package com.platenplatter.util;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;

/**
 * One meal that is about to be saved: from the Recipes tab or from one row of the meal-history CSV.
 * CSV format: date,recipe_name,category,prep_minutes[,meal_slot,calories,healthy_level]
 * The last three columns are optional, so the old 4-column files still import.
 */
public class ImportRow {
    private final String date;
    private final String recipeName;
    private final String category;
    private final int prepMinutes;
    private final MealSlot mealSlot;
    private final int calories;
    private final HealthyLevel healthyLevel;

    /** Old 4-column row: the slot, calories and healthy level get their default values. */
    public ImportRow(String date, String recipeName, String category, int prepMinutes) {
        this(date, recipeName, category, prepMinutes, null, 0, null);
    }

    /**
     * @param mealSlot     null = the default slot for the category (see MealSlot.defaultFor)
     * @param calories     0 = not known
     * @param healthyLevel null = worked out by HealthClassifier from the calories and the slot
     */
    public ImportRow(String date, String recipeName, String category, int prepMinutes,
                     MealSlot mealSlot, int calories, HealthyLevel healthyLevel) {
        this.date = date;
        this.recipeName = recipeName;
        this.category = category;
        this.prepMinutes = prepMinutes;
        this.mealSlot = mealSlot == null ? MealSlot.defaultFor(category) : mealSlot;
        this.calories = calories;
        this.healthyLevel = healthyLevel != null ? healthyLevel
                : HealthClassifier.classify(Math.max(calories, 0), this.mealSlot, category);
    }

    public String getDate() { return date; }
    public String getRecipeName() { return recipeName; }
    public String getCategory() { return category; }
    public int getPrepMinutes() { return prepMinutes; }
    public MealSlot getMealSlot() { return mealSlot; }
    public int getCalories() { return calories; }
    public HealthyLevel getHealthyLevel() { return healthyLevel; }

    /** Quick, Average or Slow (worked out from the prep minutes, never stored). */
    public PrepTier getPrepTier() {
        return PrepTier.of(Math.max(prepMinutes, 0));
    }
}
