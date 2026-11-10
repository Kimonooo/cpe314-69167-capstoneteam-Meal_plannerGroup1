package com.platenplatter.util;

import com.platenplatter.model.SavedRecipe;

/**
 * Totals for nested recipes, worked out with RECURSION and no loop.
 *
 * A recipe can hold sub-recipes (a cake holds a filling, the filling holds a cream).
 * Base case: a recipe with no sub-recipes. Its stream is empty, so the sum is 0 and
 * only its own time is returned.
 * Recursive case: its own time plus the total of each sub-recipe, to any depth.
 *
 * A loop in the tree (a recipe inside itself) can never be built: SavedRecipe.addSubRecipe
 * asks wouldCreateCycle first and throws CircularRecipeException. So these walks always end.
 */
public final class RecipeCalculator {
    private RecipeCalculator() { }

    /** Base case: no sub-recipes, so the stream is empty and its sum is 0. */
    public static int totalPrepMinutes(SavedRecipe recipe) {
        return recipe.getPrepMinutes() + recipe.getSubRecipes().stream()
                .mapToInt(RecipeCalculator::totalPrepMinutes)   // recursive case
                .sum();
    }

    /** The same pattern for calories: its own usual calories plus every sub-recipe's total. */
    public static int totalCalories(SavedRecipe recipe) {
        return recipe.getUsualCalories() + recipe.getSubRecipes().stream()
                .mapToInt(RecipeCalculator::totalCalories)
                .sum();
    }

    /** True when sub is the parent itself or already contains it at any depth. */
    public static boolean wouldCreateCycle(SavedRecipe parent, SavedRecipe sub) {
        return sub == parent || sub.getSubRecipes().stream()
                .anyMatch(inner -> wouldCreateCycle(parent, inner));
    }
}
