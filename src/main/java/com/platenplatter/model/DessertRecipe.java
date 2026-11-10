package com.platenplatter.model;

/** A dessert recipe: 15 extra minutes for chilling or setting. */
public class DessertRecipe extends SavedRecipe {
    public DessertRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories) {
        this(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, "");
    }

    public DessertRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories,
            String mealDbId) {
        super(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes() + 15; // dessert: +15 min for chilling or setting
    }

    @Override
    public String getMealType() {
        return "DESSERT";
    }
}
