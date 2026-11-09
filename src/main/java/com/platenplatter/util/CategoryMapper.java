package com.platenplatter.util;

/**
 * Turns a TheMealDB category (or a CSV category) into one of our three meal types,
 * which picks the SavedRecipe subclass: Breakfast -> BREAKFAST, Dessert -> DESSERT,
 * everything else (Chicken, Vegetarian, MainCourse, ...) -> MAIN_COURSE.
 */
public final class CategoryMapper {
    private CategoryMapper() { }

    public static String toMealType(String category) {
        String clean = category == null ? "" : category.trim();
        if (clean.equalsIgnoreCase("Breakfast")) {
            return "BREAKFAST";
        }
        if (clean.equalsIgnoreCase("Dessert")) {
            return "DESSERT";
        }
        return "MAIN_COURSE";
    }
}
