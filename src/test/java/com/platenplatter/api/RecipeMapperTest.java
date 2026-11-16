package com.platenplatter.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.SavedRecipe;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecipeMapperTest {
    private static MealData meal(String category) {
        return new MealData("7", "Test dish", category, "Filipino", "Cook it.", List.of(),
                "photo.jpg");
    }

    @Test
    void theCategoryPicksTheSubclass() {
        assertInstanceOf(DessertRecipe.class, RecipeMapper.toRecipe(meal("Dessert"), 20, 300));
        assertInstanceOf(BreakfastRecipe.class, RecipeMapper.toRecipe(meal("Breakfast"), 20, 300));
        assertInstanceOf(MainCourseRecipe.class, RecipeMapper.toRecipe(meal("Seafood"), 20, 300));
    }

    @Test
    void theDtoValuesAreKept() {
        SavedRecipe recipe = RecipeMapper.toRecipe(meal("Dessert"), 20, 300);
        assertEquals("Test dish", recipe.getRecipeName());
        assertEquals("Filipino", recipe.getCuisine());
        assertEquals(20, recipe.getBasePrepMinutes());
        assertEquals(35, recipe.getPrepMinutes());            // a dessert adds 15 minutes
        assertEquals(300, recipe.getUsualCalories());
        assertEquals("7", recipe.getMealDbId());
    }

    @Test
    void withCategoryOnlyChangesTheCategory() {
        MealData picked = RecipeMapper.withCategory(meal(""), "Breakfast");
        assertEquals("Breakfast", picked.category());
        assertEquals("photo.jpg", picked.thumbnail());
        assertEquals("Test dish", picked.name());
    }
}
