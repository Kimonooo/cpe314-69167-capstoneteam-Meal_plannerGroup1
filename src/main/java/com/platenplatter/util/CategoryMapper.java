package com.platenplatter.util;

/**
 * Maps TheMealDB's many categories (Chicken, Beef, Seafood, Starter...) onto OUR three
 * plan categories so the Analytics chart stays clean: Breakfast, MainCourse, Dessert.
 */
public final class CategoryMapper {
    private CategoryMapper() { }

    public static String toPlanCategory(String apiCategory) {
        if (apiCategory == null) {
            return "MainCourse";
        }
        String c = apiCategory.trim();
        if (c.equalsIgnoreCase("Breakfast")) {
            return "Breakfast";
        }
        if (c.equalsIgnoreCase("Dessert")) {
            return "Dessert";
        }
        return "MainCourse";
    }
}
