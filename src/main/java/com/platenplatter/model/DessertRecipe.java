package com.platenplatter.model;

public class DessertRecipe extends Recipe {
    public DessertRecipe(int id, String name, String cuisine, int basePrepMinutes, int calories) {
        super(id, name, cuisine, basePrepMinutes, calories);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes() + 15; // dessert: +15 min for chilling/setting
    }

    @Override
    public String getMealType() {
        return "Dessert";
    }
}
