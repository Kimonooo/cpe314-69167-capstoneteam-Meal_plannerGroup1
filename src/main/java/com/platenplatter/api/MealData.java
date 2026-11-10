package com.platenplatter.api;

import com.platenplatter.model.IngredientAmount;
import java.util.List;

/**
 * One recipe read from TheMealDB that is not saved yet (a search result or a recommendation).
 * filter.php only gives the id and the name, so the other fields stay empty until lookup.php fills them in.
 */
public record MealData(String mealDbId, String name, String category, String area,
                       String instructions, List<IngredientAmount> ingredients) {

    /** True when only the id and the name are known (a filter.php result). */
    public boolean isSummary() {
        return instructions.isEmpty() && ingredients.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
