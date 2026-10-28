package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.Recipe;
import org.junit.jupiter.api.Test;

class RecipeCalculatorTest {

    @Test
    void recipeWithNoSubRecipesTotalsItsOwnTime() {
        // edge case: the base case of the recursion
        Recipe oats = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        assertEquals(10, RecipeCalculator.getTotalPrepMinutes(oats));
        assertEquals(300, RecipeCalculator.getTotalCalories(oats));
    }

    @Test
    void oneSubRecipeIsAddedToTheTotal() {
        Recipe pasta = new MainCourseRecipe(1, "Pasta", "Italian", 20, 500); // 20 + 10 = 30 min
        Recipe sauce = new BreakfastRecipe(2, "Sauce", "Italian", 10, 100);  // 10 min
        pasta.addSubRecipe(sauce);

        assertEquals(40, RecipeCalculator.getTotalPrepMinutes(pasta));
        assertEquals(600, RecipeCalculator.getTotalCalories(pasta));
    }

    @Test
    void threeLevelNestingAddsEveryLevel() {
        Recipe top = new DessertRecipe(1, "Cake", "American", 30, 400);     // 30 + 15 = 45
        Recipe middle = new MainCourseRecipe(2, "Filling", "French", 20, 550); // 20 + 10 = 30
        Recipe bottom = new BreakfastRecipe(3, "Cream", "French", 10, 300);  // 10
        top.addSubRecipe(middle);
        middle.addSubRecipe(bottom);

        assertEquals(85, RecipeCalculator.getTotalPrepMinutes(top));
        assertEquals(1250, RecipeCalculator.getTotalCalories(top));
    }

    @Test
    void sameSubRecipeUsedTwiceIsCountedTwiceAndIsNotACycle() {
        // edge case: using one recipe twice is fine, it is not a loop
        Recipe top = new BreakfastRecipe(1, "Platter", "American", 10, 100);
        Recipe shared = new BreakfastRecipe(2, "Toast", "American", 10, 50);
        top.addSubRecipe(shared);
        top.addSubRecipe(shared);

        assertEquals(30, RecipeCalculator.getTotalPrepMinutes(top));
    }

    @Test
    void recipeContainingItselfIsRejected() {
        // edge case: would loop forever without protection
        Recipe loop = new BreakfastRecipe(1, "Loop", "American", 10, 100);
        loop.addSubRecipe(loop);

        assertThrows(CircularRecipeException.class,
                () -> RecipeCalculator.getTotalPrepMinutes(loop));
    }

    @Test
    void twoRecipesContainingEachOtherAreRejected() {
        // edge case: A contains B and B contains A
        Recipe a = new BreakfastRecipe(1, "A", "American", 10, 100);
        Recipe b = new MainCourseRecipe(2, "B", "American", 10, 100);
        a.addSubRecipe(b);
        b.addSubRecipe(a);

        assertThrows(CircularRecipeException.class,
                () -> RecipeCalculator.getTotalCalories(a));
    }

    @Test
    void difficultyLabelChangesAtTheBoundaries() {
        // edge cases: exactly on each cut-off
        assertEquals("Quick", RecipeCalculator.getDifficultyLabel(30));
        assertEquals("Easy", RecipeCalculator.getDifficultyLabel(31));
        assertEquals("Easy", RecipeCalculator.getDifficultyLabel(60));
        assertEquals("Hard", RecipeCalculator.getDifficultyLabel(61));
    }

    @Test
    void wouldCreateCycleSpotsLoopsBeforeTheyHappen() {
        Recipe a = new BreakfastRecipe(1, "A", "American", 10, 100);
        Recipe b = new BreakfastRecipe(2, "B", "American", 10, 100);
        Recipe other = new BreakfastRecipe(3, "Other", "American", 10, 100);
        a.addSubRecipe(b); // A contains B

        assertTrue(RecipeCalculator.wouldCreateCycle(b, a));     // putting A inside B makes a loop
        assertTrue(RecipeCalculator.wouldCreateCycle(a, a));     // a recipe inside itself
        assertFalse(RecipeCalculator.wouldCreateCycle(a, other)); // unrelated recipe is fine
    }
}
