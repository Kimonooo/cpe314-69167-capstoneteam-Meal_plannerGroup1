package com.platenplatter.api;

import com.platenplatter.model.IngredientAmount;
import java.util.List;

/**
 * One recipe read from TheMealDB that is not saved yet (a search result or a recommendation).
 * filter.php only gives the id, the name and the photo, so the other fields stay empty until
 * lookup.php fills them in. thumbnail is the web address of the meal's photo (strMealThumb),
 * or "" when TheMealDB gives none.
 */
public record MealData(String mealDbId, String name, String category, String area,
                       String instructions, List<IngredientAmount> ingredients, String thumbnail) {

    /** A missing photo address becomes "", so the screens never meet a null. */
    public MealData {
        thumbnail = thumbnail == null ? "" : thumbnail.trim();
    }

    /** A recipe without a photo (used by the tests and for meals typed by hand). */
    public MealData(String mealDbId, String name, String category, String area,
                    String instructions, List<IngredientAmount> ingredients) {
        this(mealDbId, name, category, area, instructions, ingredients, "");
    }

    public boolean hasPhoto() {
        return !thumbnail.isEmpty();
    }

    /** True when only the id and the name are known (a filter.php result). */
    public boolean isSummary() {
        return instructions.isEmpty() && ingredients.isEmpty();
    }

    @Override
    public String toString() {
        return name;
    }
}
