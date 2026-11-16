package com.platenplatter.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collector;

/**
 * Builds the shopping list from the ingredient amounts of the planned meals.
 * The user chooses ONE recipe and gets only that recipe's ingredients, or chooses
 * "All planned recipes", where the same ingredient from two recipes is listed once with both measures.
 */
public final class ShoppingList {
    private ShoppingList() { }

    /** One line of the "Which recipe?" box: a saved recipe, shown by its name. */
    public record RecipeChoice(long savedRecipeId, String name) {
        @Override
        public String toString() {
            return name;
        }
    }

    /** The choice that puts every planned recipe on one list. */
    public static final RecipeChoice ALL_RECIPES = new RecipeChoice(0, "All planned recipes");

    /** The recipes of the planned meals, A to Z, each one once even when it is planned on several days. */
    public static List<RecipeChoice> recipeChoices(List<PlannedMeal> meals) {
        return meals.stream()
                .map(meal -> new RecipeChoice(meal.getRecipe().getSavedRecipeId(), meal.getRecipeName()))
                .distinct()
                .sorted(Comparator.comparing((RecipeChoice choice) -> choice.name(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /**
     * Only the ingredient amounts of one recipe. A recipe planned on two days comes back from the
     * database twice, so each amount is kept once: cooking it twice does not show "garlic (3 cloves, 3 cloves)".
     */
    public static List<IngredientAmount> forRecipe(List<IngredientAmount> items, long savedRecipeId) {
        return distinct(items.stream().filter(item -> item.getSavedRecipeId() == savedRecipeId).toList());
    }

    /**
     * Each ingredient amount once. The database returns a recipe's amounts once for every day it is
     * planned, so "All planned recipes" would otherwise show "garlic (3 cloves, 3 cloves)".
     */
    public static List<IngredientAmount> distinct(List<IngredientAmount> items) {
        Set<Long> seen = new HashSet<Long>();
        return items.stream()
                .filter(item -> seen.add(item.getIngredientAmountId()))
                .toList();
    }

    /** Only the planned meals that cook one recipe. */
    public static List<PlannedMeal> mealsOf(List<PlannedMeal> meals, long savedRecipeId) {
        return meals.stream()
                .filter(meal -> meal.getRecipe().getSavedRecipeId() == savedRecipeId)
                .toList();
    }

    /** Food group -> ingredient -> measures, e.g. garlic -> [3 cloves, 1 head]. */
    public static Map<FoodGroup, Map<String, List<String>>> build(
            List<IngredientAmount> items) {
        // Explicit types so both javac (Maven) and the Eclipse compiler accept this
        Collector<IngredientAmount, ?, Map<String, List<String>>> byName = groupingBy(
                (IngredientAmount item) -> item.getIngredient().getIngredientName().toLowerCase(),
                () -> new TreeMap<String, List<String>>(),    // A to Z in a group
                mapping(IngredientAmount::getMeasure, toList()));
        return items.stream().collect(groupingBy(
                (IngredientAmount item) -> item.getIngredient().getFoodGroup(),
                // groups in the fixed FoodGroup order
                () -> new EnumMap<FoodGroup, Map<String, List<String>>>(FoodGroup.class),
                byName));
    }

    /** The planned recipes that have no ingredients saved yet (sorted, no repeats). */
    public static List<String> missingRecipes(List<PlannedMeal> meals, List<IngredientAmount> items) {
        Set<Long> withIngredients = items.stream().map(IngredientAmount::getSavedRecipeId).collect(toSet());
        return meals.stream()
                .filter(meal -> !withIngredients.contains(meal.getRecipe().getSavedRecipeId()))
                .map(PlannedMeal::getRecipeName)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    /** One line for one ingredient: "garlic (3 cloves, 1 head)". Blank measures are left out. */
    public static String line(String ingredient, List<String> measures) {
        String amounts = measures.stream().filter(m -> !m.isBlank()).collect(joining(", "));
        return amounts.isEmpty() ? ingredient : ingredient + " (" + amounts + ")";
    }

    /** The whole list as plain text, for Copy and Save. */
    public static String asText(Map<FoodGroup, Map<String, List<String>>> list) {
        return list.entrySet().stream()
                .map(group -> group.getKey().getLabel() + "\n" + group.getValue().entrySet().stream()
                        .map(item -> "  [ ] " + line(item.getKey(), item.getValue()))
                        .collect(joining("\n")))
                .collect(joining("\n\n"));
    }

    /** How many different ingredients are on the list. */
    public static int count(Map<FoodGroup, Map<String, List<String>>> list) {
        return list.values().stream().mapToInt(Map::size).sum();
    }
}
