package com.platenplatter.util;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Builds the shopping list from the ingredient amounts of the planned meals.
 * The same ingredient from two recipes is listed once, with both measures.
 */
public final class ShoppingList {
    private ShoppingList() { }

    /** Food group -> ingredient -> measures, e.g. garlic -> [3 cloves, 1 head]. */
    public static Map<FoodGroup, Map<String, List<String>>> build(
            List<IngredientAmount> items) {
        return items.stream().collect(groupingBy(
                item -> item.getIngredient().getFoodGroup(),
                () -> new EnumMap<>(FoodGroup.class),        // groups in fixed order
                groupingBy(item -> item.getIngredient().getIngredientName().toLowerCase(),
                        TreeMap::new,                         // A to Z in a group
                        mapping(IngredientAmount::getMeasure, toList()))));
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
