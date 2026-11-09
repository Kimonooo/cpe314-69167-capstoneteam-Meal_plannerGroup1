package com.platenplatter.model;

import com.platenplatter.util.CircularRecipeException;
import com.platenplatter.util.RecipeCalculator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * One row of the saved_recipes table: a recipe the user has saved.
 *
 * ABSTRACT: there is no plain recipe. Every recipe is a BreakfastRecipe, MainCourseRecipe or
 * DessertRecipe (the meal_type column says which), and each subclass adds its own prep time.
 * Code that calls getPrepMinutes() on a SavedRecipe never checks the subclass: polymorphism.
 *
 * Sub-recipes (a cake holding a filling) are only built in memory, to show recursion.
 */
public abstract class SavedRecipe {
    private final long savedRecipeId;
    private final String recipeName;
    private final String cuisine;
    private final int basePrepMinutes;
    private final int usualCalories;
    private final String mealDbId;
    private final List<SavedRecipe> subRecipes = new ArrayList<>();

    protected SavedRecipe(long savedRecipeId, String recipeName, String cuisine,
                          int basePrepMinutes, int usualCalories, String mealDbId) {
        if (recipeName == null || recipeName.isBlank()) {
            throw new IllegalArgumentException("Recipe name cannot be empty");
        }
        if (basePrepMinutes < 0 || usualCalories < 0) {
            throw new IllegalArgumentException("Minutes and calories cannot be negative");
        }
        this.savedRecipeId = savedRecipeId;
        this.recipeName = recipeName.trim();
        this.cuisine = cuisine == null ? "" : cuisine.trim();
        this.basePrepMinutes = basePrepMinutes;
        this.usualCalories = usualCalories;
        this.mealDbId = mealDbId == null ? "" : mealDbId.trim();
    }

    /** The base minutes plus this type's own extra time. */
    public abstract int getPrepMinutes();

    /** The meal_type saved in the database: BREAKFAST, MAIN_COURSE or DESSERT. */
    public abstract String getMealType();

    /** Puts a recipe inside this one. A recipe can never end up inside itself. */
    public void addSubRecipe(SavedRecipe sub) {
        if (RecipeCalculator.wouldCreateCycle(this, sub)) {
            throw new CircularRecipeException(this, sub);
        }
        subRecipes.add(sub);
    }

    public List<SavedRecipe> getSubRecipes() {
        return Collections.unmodifiableList(subRecipes);
    }

    public long getSavedRecipeId() { return savedRecipeId; }
    public String getRecipeName() { return recipeName; }
    public String getCuisine() { return cuisine; }
    public int getBasePrepMinutes() { return basePrepMinutes; }
    public int getUsualCalories() { return usualCalories; }
    public String getMealDbId() { return mealDbId; }

    /** "Breakfast", "Main Course" or "Dessert", for the screens. */
    public String getMealTypeLabel() {
        return switch (getMealType()) {
            case "BREAKFAST" -> "Breakfast";
            case "DESSERT" -> "Dessert";
            default -> "Main Course";
        };
    }

    /** Makes the right subclass from the meal_type text (the factory the DAO and the screens use). */
    public static SavedRecipe create(String mealType, long savedRecipeId, String recipeName, String cuisine,
                                     int basePrepMinutes, int usualCalories, String mealDbId) {
        String type = mealType == null ? "" : mealType.trim().toUpperCase(Locale.ROOT);
        return switch (type) {
            case "BREAKFAST" -> new BreakfastRecipe(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
            case "DESSERT" -> new DessertRecipe(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
            default -> new MainCourseRecipe(savedRecipeId, recipeName, cuisine, basePrepMinutes, usualCalories, mealDbId);
        };
    }

    @Override
    public String toString() {
        return getMealTypeLabel() + ": " + recipeName + " (" + getPrepMinutes() + " min)";
    }
}
