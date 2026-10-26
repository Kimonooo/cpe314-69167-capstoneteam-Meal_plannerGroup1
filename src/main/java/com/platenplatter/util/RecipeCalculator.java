package com.platenplatter.util;

import com.platenplatter.model.Recipe;

/**
 * Totals values across a recipe and all of its nested sub-recipes.
 * A sub-recipe can itself have sub-recipes, to any depth, so recursion is needed.
 */
public class RecipeCalculator {

    private RecipeCalculator() { }

    /**
     * Total prep minutes = this recipe's own time + the total of every sub-recipe.
     * BASE CASE: a recipe with no sub-recipes just returns its own time.
     * RECURSIVE CASE: otherwise it adds the total of each sub-recipe.
     * (Day 2: add protection against circular references.)
     */
    public static int getTotalPrepMinutes(Recipe recipe) {
        int total = recipe.getPrepMinutes();
        for (Recipe sub : recipe.getSubRecipes()) {
            total += getTotalPrepMinutes(sub);
        }
        return total;
    }

    /** Same idea for calories. */
    public static int getTotalCalories(Recipe recipe) {
        int total = recipe.getCalories();
        for (Recipe sub : recipe.getSubRecipes()) {
            total += getTotalCalories(sub);
        }
        return total;
    }

    /**
     * Quick / Easy / Hard from the total prep time.
     * These cut-offs are placeholders: the group can change them.
     */
    public static String getDifficultyLabel(int totalMinutes) {
        if (totalMinutes <= 30) {
            return "Quick";
        } else if (totalMinutes <= 60) {
            return "Easy";
        }
        return "Hard";
    }
}
