package com.platenplatter.util;

import com.platenplatter.model.Recipe;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

/**
 * Totals values across a recipe and all of its nested sub-recipes (to any depth).
 * Day 2: it now notices when a recipe contains itself and stops instead of looping forever.
 */
public class RecipeCalculator {

    private RecipeCalculator() { }

    /** Own prep time + the total of every sub-recipe. Throws CircularRecipeException on a loop. */
    public static int getTotalPrepMinutes(Recipe recipe) {
        return totalPrepMinutes(recipe, newPath());
    }

    /** Same idea for calories. */
    public static int getTotalCalories(Recipe recipe) {
        return totalCalories(recipe, newPath());
    }

    /**
     * BASE CASE: a recipe with no sub-recipes returns just its own time.
     * RECURSIVE CASE: otherwise it adds the total of each sub-recipe.
     * "path" remembers the recipes we are currently inside. If we meet one of them again,
     * the recipe contains itself, which would loop forever, so we stop.
     */
    private static int totalPrepMinutes(Recipe recipe, Set<Recipe> path) {
        enter(recipe, path);
        int total = recipe.getPrepMinutes();
        for (Recipe sub : recipe.getSubRecipes()) {
            total += totalPrepMinutes(sub, path);
        }
        path.remove(recipe); // we are leaving this recipe, so it may appear again elsewhere
        return total;
    }

    private static int totalCalories(Recipe recipe, Set<Recipe> path) {
        enter(recipe, path);
        int total = recipe.getCalories();
        for (Recipe sub : recipe.getSubRecipes()) {
            total += totalCalories(sub, path);
        }
        path.remove(recipe);
        return total;
    }

    private static void enter(Recipe recipe, Set<Recipe> path) {
        if (!path.add(recipe)) {
            throw new CircularRecipeException(
                    "Circular reference: \"" + recipe.getName() + "\" contains itself");
        }
    }

    /**
     * Ask this BEFORE adding "candidate" inside "parent".
     * True means it would make a loop (candidate is the parent, or already contains the parent).
     */
    public static boolean wouldCreateCycle(Recipe parent, Recipe candidate) {
        return candidate == parent || contains(candidate, parent, newPath());
    }

    /** Recursive search: is "target" anywhere inside "root"? */
    private static boolean contains(Recipe root, Recipe target, Set<Recipe> visited) {
        if (root == target) {
            return true;
        }
        if (!visited.add(root)) {
            return false; // already searched this one
        }
        for (Recipe sub : root.getSubRecipes()) {
            if (contains(sub, target, visited)) {
                return true;
            }
        }
        return false;
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

    // Recipes are compared by identity (same object), not by name
    private static Set<Recipe> newPath() {
        return Collections.newSetFromMap(new IdentityHashMap<>());
    }
}
