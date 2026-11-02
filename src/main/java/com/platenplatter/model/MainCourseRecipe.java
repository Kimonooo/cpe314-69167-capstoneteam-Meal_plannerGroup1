package com.platenplatter.model;

public class MainCourseRecipe extends Recipe {
    public MainCourseRecipe(int id, String name, String cuisine, int basePrepMinutes, int calories) {
        super(id, name, cuisine, basePrepMinutes, calories);
    }

    @Override
    public int getPrepMinutes() {
        return getBasePrepMinutes() + 10; // main course: +10 min for cooking setup
    }

    @Override
    public String getMealType() {
        return "MainCourse";
    }
}
