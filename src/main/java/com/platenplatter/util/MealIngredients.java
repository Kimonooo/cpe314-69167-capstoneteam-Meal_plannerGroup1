package com.platenplatter.util;

import java.util.List;

/** One recipe in the shopping range: its name, how many times it is planned, and its ingredients. */
public class MealIngredients {
    private final String recipeName;
    private final int timesPlanned;
    private final List<String> ingredients;

    public MealIngredients(String recipeName, int timesPlanned, List<String> ingredients) {
        this.recipeName = recipeName;
        this.timesPlanned = timesPlanned;
        this.ingredients = ingredients;
    }

    public String getRecipeName() { return recipeName; }
    public int getTimesPlanned() { return timesPlanned; }
    public List<String> getIngredients() { return ingredients; }
}
