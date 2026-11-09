package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class SavedRecipeTest {

    @Test
    void eachSubclassAddsItsOwnPrepTime() {
        // every variable is a SavedRecipe, but each one behaves like its own subclass (polymorphism)
        List<SavedRecipe> recipes = List.of(
                new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300),
                new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550),
                new DessertRecipe(3, "Chocolate Cake", "American", 30, 400));
        assertEquals(List.of(10, 30, 45), recipes.stream().map(SavedRecipe::getPrepMinutes).toList());
    }

    @Test
    void theMealTypeIsWhatTheDatabaseStores() {
        assertEquals("BREAKFAST", new BreakfastRecipe(1, "Oats", "", 5, 0).getMealType());
        assertEquals("MAIN_COURSE", new MainCourseRecipe(1, "Adobo", "", 5, 0).getMealType());
        assertEquals("DESSERT", new DessertRecipe(1, "Leche Flan", "", 5, 0).getMealType());
        assertEquals("Main Course", new MainCourseRecipe(1, "Adobo", "", 5, 0).getMealTypeLabel());
    }

    @Test
    void theFactoryMakesTheRightSubclassFromMealType() {
        assertInstanceOf(DessertRecipe.class, SavedRecipe.create("DESSERT", 7, "Leche Flan", "Filipino", 35, 350, ""));
        assertInstanceOf(BreakfastRecipe.class, SavedRecipe.create("breakfast", 7, "Tapsilog", "", 20, 0, ""));
        assertInstanceOf(MainCourseRecipe.class, SavedRecipe.create(null, 7, "Adobo", "", 45, 0, ""));
        assertEquals(7, SavedRecipe.create("DESSERT", 7, "Leche Flan", "Filipino", 35, 350, "").getSavedRecipeId());
    }

    @Test
    void aBlankNameOrNegativeNumbersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MainCourseRecipe(1, "  ", "", 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new MainCourseRecipe(1, "Adobo", "", -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new DessertRecipe(1, "Cake", "", 10, -5));
    }

    @Test
    void namesAreTrimmedAndMissingTextBecomesEmpty() {
        SavedRecipe recipe = SavedRecipe.create("MAIN_COURSE", 0, "  Chicken Adobo ", null, 45, 0, null);
        assertEquals("Chicken Adobo", recipe.getRecipeName());
        assertEquals("", recipe.getCuisine());
        assertEquals("", recipe.getMealDbId());
        assertTrue(recipe.getSubRecipes().isEmpty());
    }
}
