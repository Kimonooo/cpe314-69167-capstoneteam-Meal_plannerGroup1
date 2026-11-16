package com.platenplatter.api;

import com.platenplatter.model.SavedRecipe;
import com.platenplatter.util.CategoryMapper;

/**
 * Turns the API's data object (MealData, a DTO that is never saved) into our own domain objects.
 * The screens never build a SavedRecipe themselves; they ask this mapper (through the controller).
 */
public final class RecipeMapper {
    private RecipeMapper() { }

    /**
     * The SavedRecipe for a TheMealDB meal. Its category picks the subclass (Breakfast, Dessert,
     * else Main Course), so getPrepMinutes() adds that type's own time to the base minutes.
     */
    public static SavedRecipe toRecipe(MealData meal, int basePrepMinutes, int calories) {
        return SavedRecipe.create(CategoryMapper.toMealType(meal.category()), 0, meal.name(),
                meal.area(), basePrepMinutes, calories, meal.mealDbId());
    }

    /** The same meal with another category (filter.php gives none, so the pick's own is used). */
    public static MealData withCategory(MealData meal, String category) {
        return new MealData(meal.mealDbId(), meal.name(), category, meal.area(),
                meal.instructions(),
                meal.ingredients(), meal.thumbnail());
    }
}
