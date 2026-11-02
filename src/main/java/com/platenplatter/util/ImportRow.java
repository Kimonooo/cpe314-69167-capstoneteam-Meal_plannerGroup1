package com.platenplatter.util;

/** One valid row of the meal-plan history CSV: date,recipe_name,category,prep_minutes */
public class ImportRow {
    private final String date;
    private final String recipeName;
    private final String category;
    private final int prepMinutes;

    public ImportRow(String date, String recipeName, String category, int prepMinutes) {
        this.date = date;
        this.recipeName = recipeName;
        this.category = category;
        this.prepMinutes = prepMinutes;
    }

    public String getDate() { return date; }
    public String getRecipeName() { return recipeName; }
    public String getCategory() { return category; }
    public int getPrepMinutes() { return prepMinutes; }
}
