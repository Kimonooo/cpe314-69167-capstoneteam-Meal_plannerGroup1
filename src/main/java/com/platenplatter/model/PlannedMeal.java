package com.platenplatter.model;

import java.time.LocalDate;

/**
 * One row of the planned_meals table: one recipe planned for one meal slot on one day.
 * It holds the SavedRecipe it plans (read with a JOIN), so a screen can show the recipe's name.
 * A CSV row, a recipe chosen on the Recipes page and a row read from SQLite all become this class.
 */
public class PlannedMeal {
    private final long plannedMealId;
    private final long userId;
    private final SavedRecipe recipe;
    private final LocalDate planDate;
    private final MealSlot mealSlot;
    private final int prepMinutes;
    private final int calories;
    private final HealthyLevel healthyLevel;

    public PlannedMeal(long plannedMealId, long userId, SavedRecipe recipe, LocalDate planDate,
                       MealSlot mealSlot, int prepMinutes, int calories, HealthyLevel healthyLevel) {
        if (recipe == null || planDate == null || mealSlot == null || healthyLevel == null) {
            throw new IllegalArgumentException("A planned meal needs a recipe, a date, a meal slot and a healthy level");
        }
        if (prepMinutes < 0 || calories < 0) {
            throw new IllegalArgumentException("Minutes and calories cannot be negative");
        }
        this.plannedMealId = plannedMealId;
        this.userId = userId;
        this.recipe = recipe;
        this.planDate = planDate;
        this.mealSlot = mealSlot;
        this.prepMinutes = prepMinutes;
        this.calories = calories;
        this.healthyLevel = healthyLevel;
    }

    public long getPlannedMealId() { return plannedMealId; }
    public long getUserId() { return userId; }
    public SavedRecipe getRecipe() { return recipe; }
    public LocalDate getPlanDate() { return planDate; }
    public MealSlot getMealSlot() { return mealSlot; }
    public int getPrepMinutes() { return prepMinutes; }
    public int getCalories() { return calories; }
    public HealthyLevel getHealthyLevel() { return healthyLevel; }

    /** Shortcut for getRecipe().getRecipeName(). */
    public String getRecipeName() {
        return recipe.getRecipeName();
    }

    /** Quick, Average or Slow, always worked out from the minutes (never stored). */
    public PrepTier getPrepTier() {
        return PrepTier.of(prepMinutes);
    }

    /** The same meal after it was saved: with its new id, its owner and the saved recipe. */
    public PlannedMeal saved(long newPlannedMealId, long ownerId, SavedRecipe savedRecipe) {
        return new PlannedMeal(newPlannedMealId, ownerId, savedRecipe, planDate, mealSlot, prepMinutes, calories, healthyLevel);
    }

    @Override
    public String toString() {
        return planDate + " " + mealSlot.getLabel() + ": " + recipe.getRecipeName();
    }
}
