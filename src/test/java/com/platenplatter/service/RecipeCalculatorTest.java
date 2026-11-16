package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import org.junit.jupiter.api.Test;

class RecipeCalculatorTest {

    @Test
    void aRecipeWithNoSubRecipesIsTheBaseCase() {
        SavedRecipe oats = new BreakfastRecipe(1, "Oats", "", 10, 300);
        assertEquals(10, RecipeCalculator.totalPrepMinutes(oats));
        assertEquals(300, RecipeCalculator.totalCalories(oats));
    }

    @Test
    void threeLevelNestingAddsEveryLevel() {
        SavedRecipe cake = new DessertRecipe(1, "Cake", "", 30, 400);         // 30 + 15 = 45
        SavedRecipe filling = new MainCourseRecipe(2, "Filling", "", 20, 450); // 20 + 10 = 30
        SavedRecipe cream = new BreakfastRecipe(3, "Cream", "", 10, 400);      // 10
        filling.addSubRecipe(cream);
        cake.addSubRecipe(filling);
        assertEquals(85, RecipeCalculator.totalPrepMinutes(cake));
        assertEquals(1250, RecipeCalculator.totalCalories(cake));
        assertEquals(PrepTier.SLOW, PrepTier.of(RecipeCalculator.totalPrepMinutes(cake)));
    }

    @Test
    void siblingsAreAllCounted() {
        SavedRecipe plate = new MainCourseRecipe(1, "Plate", "", 0, 0);       // 10
        plate.addSubRecipe(new BreakfastRecipe(2, "Rice", "", 15, 200));
        plate.addSubRecipe(new BreakfastRecipe(3, "Egg", "", 5, 90));
        assertEquals(30, RecipeCalculator.totalPrepMinutes(plate));
        assertEquals(290, RecipeCalculator.totalCalories(plate));
    }

    @Test
    void theSameSubRecipeCanBeUsedTwice() {
        SavedRecipe sauce = new BreakfastRecipe(1, "Sauce", "", 5, 50);
        SavedRecipe meal = new MainCourseRecipe(2, "Meal", "", 10, 0);
        meal.addSubRecipe(sauce);
        meal.addSubRecipe(sauce);                      // not a cycle: the sauce does not contain the meal
        assertEquals(30, RecipeCalculator.totalPrepMinutes(meal));
    }

    @Test
    void aRecipeCannotContainItself() {
        SavedRecipe cake = new DessertRecipe(1, "Cake", "", 30, 400);
        assertThrows(CircularRecipeException.class, () -> cake.addSubRecipe(cake));
        assertTrue(cake.getSubRecipes().isEmpty());
    }

    @Test
    void aRecipeCannotContainOneThatAlreadyContainsIt() {
        SavedRecipe cake = new DessertRecipe(1, "Cake", "", 30, 400);
        SavedRecipe filling = new MainCourseRecipe(2, "Filling", "", 20, 450);
        SavedRecipe cream = new BreakfastRecipe(3, "Cream", "", 10, 400);
        cake.addSubRecipe(filling);
        filling.addSubRecipe(cream);
        CircularRecipeException e = assertThrows(CircularRecipeException.class, () -> cream.addSubRecipe(cake));
        assertTrue(e.getMessage().contains("Cake"));
        assertEquals(85, RecipeCalculator.totalPrepMinutes(cake));   // the tree is unchanged
    }

    @Test
    void wouldCreateCycleLooksAtEveryDepth() {
        SavedRecipe a = new MainCourseRecipe(1, "A", "", 1, 0);
        SavedRecipe b = new MainCourseRecipe(2, "B", "", 1, 0);
        SavedRecipe c = new MainCourseRecipe(3, "C", "", 1, 0);
        a.addSubRecipe(b);
        b.addSubRecipe(c);
        assertTrue(RecipeCalculator.wouldCreateCycle(c, a));
        assertFalse(RecipeCalculator.wouldCreateCycle(a, c));
    }
}
