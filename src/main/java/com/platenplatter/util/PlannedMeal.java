package com.platenplatter.util;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;

import java.time.LocalDate;

/** One meal that is planned on a day, as stored in the database (it has an id so it can be removed). */
public class PlannedMeal {
    private final long id;
    private final String date;
    private final String recipeName;
    private final String category;
    private final int prepMinutes;
    private final MealSlot mealSlot;
    private final int calories;
    private final HealthyLevel healthyLevel;

    /** Old 5-value meal: the slot, calories and healthy level get their default values. */
    public PlannedMeal(long id, String date, String recipeName, String category, int prepMinutes) {
        this(id, date, recipeName, category, prepMinutes, null, 0, null);
    }

    public PlannedMeal(long id, String date, String recipeName, String category, int prepMinutes,
                       MealSlot mealSlot, int calories, HealthyLevel healthyLevel) {
        this.id = id;
        this.date = date;
        this.recipeName = recipeName;
        this.category = category;
        this.prepMinutes = prepMinutes;
        this.mealSlot = mealSlot == null ? MealSlot.defaultFor(category) : mealSlot;
        this.calories = calories;
        this.healthyLevel = healthyLevel != null ? healthyLevel
                : HealthClassifier.classify(Math.max(calories, 0), this.mealSlot, category);
    }

    public long getId() { return id; }
    public String getDate() { return date; }
    public String getRecipeName() { return recipeName; }
    public String getCategory() { return category; }
    public int getPrepMinutes() { return prepMinutes; }
    public MealSlot getMealSlot() { return mealSlot; }
    /** 0 means the calories are not known. */
    public int getCalories() { return calories; }
    public HealthyLevel getHealthyLevel() { return healthyLevel; }

    /** Quick, Average or Slow (worked out from the prep minutes, never stored). */
    public PrepTier getPrepTier() {
        return PrepTier.of(Math.max(prepMinutes, 0));
    }

    public LocalDate getLocalDate() {
        return LocalDate.parse(date);
    }
}
