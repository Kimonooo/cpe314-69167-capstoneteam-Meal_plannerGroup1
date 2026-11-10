package com.platenplatter.model;

/** A main-course recipe: 10 extra minutes for cooking setup. */
public class MainCourseRecipe extends SavedRecipe {
    public MainCourseRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories) {
        this(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, "");
    }

    public MainCourseRecipe(long savedRecipeId, String recipeName, String cuisine, int basePrepMinutes, int usualCalories,
            String mealDbId) {
        super(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes() + 10; // main course: +10 min for cooking setup
    }

    @Override
    public String getMealType() {
        return "MAIN_COURSE";
    }
}
