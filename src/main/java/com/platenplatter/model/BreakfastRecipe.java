package com.platenplatter.model;

public class BreakfastRecipe extends Recipe {
    public BreakfastRecipe(int id, String name, String cuisine, int basePrepMinutes, int calories) {
        super(id, name, cuisine, basePrepMinutes, calories);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes(); // breakfast: no extra time
    }

    @Override
    public String getMealType() {
        return "Breakfast";
    }
}
