package com.platenplatter.model;

/** A breakfast recipe: its prep time is just the base minutes. */
public class BreakfastRecipe extends SavedRecipe {
    public BreakfastRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories) {
        this(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, "");
    }

    public BreakfastRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories,
            String mealDbId) {
        super(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes(); // breakfast: no extra time
    }

    @Override
    public String getMealType() {
        return "BREAKFAST";
    }
}
