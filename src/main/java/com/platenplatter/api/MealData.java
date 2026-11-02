package com.platenplatter.api;

import java.util.List;

/** Plain data holder for one meal returned by TheMealDB. */
public class MealData {
    private final String id;
    private final String name;
    private final String category;
    private final String area;
    private final String instructions;
    private final List<String> ingredients;

    public MealData(String id, String name, String category, String area,
                    String instructions, List<String> ingredients) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.area = area;
        this.instructions = instructions;
        this.ingredients = ingredients;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getArea() { return area; }
    public String getInstructions() { return instructions; }
    public List<String> getIngredients() { return ingredients; }

    @Override
    public String toString() {
        return name + " (" + category + ", " + area + ")";
    }
}
