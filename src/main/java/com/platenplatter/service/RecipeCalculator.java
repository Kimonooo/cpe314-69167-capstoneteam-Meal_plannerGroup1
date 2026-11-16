package com.platenplatter.service;

import com.platenplatter.model.SavedRecipe;
import java.util.function.ToIntFunction;

/**
 * Totals for nested recipes, worked out with RECURSION and no loop.
 *
 * A recipe can hold sub-recipes (a cake holds a filling, the filling holds a cream).
 * Base case: a recipe with no sub-recipes. Its stream is empty, so the sum is 0 and
 * only its own time is returned.
 * Recursive case: its own time plus the total of each sub-recipe, to any depth.
 *
 * A loop in the tree (a recipe inside itself) can never be built: SavedRecipe.addSubRecipe
 * checks SavedRecipe.contains first and throws CircularRecipeException. So these walks always end.
 */
public final class RecipeCalculator {
    private RecipeCalculator() { }

    /** Base case: no sub-recipes, so the stream is empty and its sum is 0. */
    public static int totalPrepMinutes(SavedRecipe recipe) {
        return totalOf(recipe, SavedRecipe::getPrepMinutes);
    }

    /** The same pattern for calories: its own usual calories plus every sub-recipe's total. */
    public static int totalCalories(SavedRecipe recipe) {
        return totalOf(recipe, SavedRecipe::getUsualCalories);
    }

    /**
     * The one recursive walk behind both totals: this recipe's own value plus the total of each
     * sub-recipe, to any depth. The Meal Plan page uses it too: a day is a tree (the day holds a
     * group per meal slot, each group holds that slot's meals), and this adds it up.
     */
    public static int totalOf(SavedRecipe recipe, ToIntFunction<SavedRecipe> value) {
        return value.applyAsInt(recipe) + recipe.getSubRecipes().stream()
                .mapToInt(sub -> totalOf(sub, value))          // recursive case
                .sum();
    }

    /** True when sub is the parent itself or already contains it at any depth. */
    public static boolean wouldCreateCycle(SavedRecipe parent, SavedRecipe sub) {
        return sub == parent || sub.contains(parent);      // SavedRecipe.contains is recursive too
    }
}
