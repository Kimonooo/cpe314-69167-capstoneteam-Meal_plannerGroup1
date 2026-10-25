package com.platenplatter.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Abstract parent of all recipes. Each subclass has its own prep-time rule. */
public abstract class Recipe {
    private final int id;
    private final String name;
    private final String cuisine;
    private final int basePrepMinutes;
    private final int calories;
    private final List<Recipe> subRecipes = new ArrayList<>();

    protected Recipe(int id, String name, String cuisine, int basePrepMinutes, int calories) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Recipe name cannot be empty");
        }
        if (basePrepMinutes < 0 || calories < 0) {
            throw new IllegalArgumentException("Minutes and calories cannot be negative");
        }
        this.id = id;
        this.name = name;
        this.cuisine = cuisine;
        this.basePrepMinutes = basePrepMinutes;
        this.calories = calories;
    }

    /** Each subclass decides how its own prep time is computed. */
    public abstract int getPrepMinutes();

    /** Each subclass says which meal type it is. */
    public abstract String getMealType();

    public void addSubRecipe(Recipe sub) {
        subRecipes.add(sub);
    }

    public List<Recipe> getSubRecipes() {
        return Collections.unmodifiableList(subRecipes);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public String getCuisine() { return cuisine; }
    public int getBasePrepMinutes() { return basePrepMinutes; }
    public int getCalories() { return calories; }

    @Override
    public String toString() {
        return getMealType() + ": " + name + " (" + getPrepMinutes() + " min)";
    }
}
