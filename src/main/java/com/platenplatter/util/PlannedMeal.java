package com.platenplatter.util;

import java.time.LocalDate;

/** One meal that is planned on a day, as stored in the database (it has an id so it can be removed). */
public class PlannedMeal {
    private final long id;
    private final String date;
    private final String recipeName;
    private final String category;
    private final int prepMinutes;

    public PlannedMeal(long id, String date, String recipeName, String category, int prepMinutes) {
        this.id = id;
        this.date = date;
        this.recipeName = recipeName;
        this.category = category;
        this.prepMinutes = prepMinutes;
    }

    public long getId() { return id; }
    public String getDate() { return date; }
    public String getRecipeName() { return recipeName; }
    public String getCategory() { return category; }
    public int getPrepMinutes() { return prepMinutes; }

    public LocalDate getLocalDate() {
        return LocalDate.parse(date);
    }
}
