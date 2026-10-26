package com.platenplatter.model;

import java.time.LocalDate;

/** One planned meal: which recipe, on which date, in which slot. */
public class MealPlanEntry {
    private final int id;
    private final LocalDate planDate;
    private final String mealSlot; // Breakfast, Lunch, Dinner, Snack
    private final Recipe recipe;
    private boolean eaten;

    public MealPlanEntry(int id, LocalDate planDate, String mealSlot, Recipe recipe) {
        if (planDate == null || recipe == null) {
            throw new IllegalArgumentException("Date and recipe are required");
        }
        if (mealSlot == null || mealSlot.isBlank()) {
            throw new IllegalArgumentException("Meal slot is required");
        }
        this.id = id;
        this.planDate = planDate;
        this.mealSlot = mealSlot;
        this.recipe = recipe;
        this.eaten = false;
    }

    public void markEaten() { this.eaten = true; }

    public int getId() { return id; }
    public LocalDate getPlanDate() { return planDate; }
    public String getMealSlot() { return mealSlot; }
    public Recipe getRecipe() { return recipe; }
    public boolean isEaten() { return eaten; }

    @Override
    public String toString() {
        return planDate + " " + mealSlot + ": " + recipe.getName();
    }
}
