package com.platenplatter.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Looks at the meals planned between two days, finds each recipe's ingredients and builds the list.
 * Where the ingredients come from: first what we SAVED when the recipe was added from the Recipes tab,
 * then the built-in list for our sample dishes (SampleIngredients). Recipes with neither are reported
 * as "missing" so the user can look them up online.
 */
public final class ShoppingPlanner {
    private ShoppingPlanner() { }

    public static ShoppingList plan(PlanStore store, LocalDate from, LocalDate to) throws Exception {
        List<PlannedMeal> meals = store.mealsBetween(from, to);

        Map<String, Integer> times = new LinkedHashMap<>();   // lower-case name -> how many times planned
        Map<String, String> names = new LinkedHashMap<>();    // lower-case name -> name as written
        for (PlannedMeal meal : meals) {
            String key = meal.getRecipeName().toLowerCase();
            times.merge(key, 1, Integer::sum);
            names.putIfAbsent(key, meal.getRecipeName());
        }

        List<MealIngredients> inputs = new ArrayList<>();
        for (Map.Entry<String, Integer> e : times.entrySet()) {
            String name = names.get(e.getKey());
            List<String> ingredients = store.ingredientsFor(name);
            if (ingredients.isEmpty()) {
                ingredients = SampleIngredients.forRecipe(name);
            }
            inputs.add(new MealIngredients(name, e.getValue(), ingredients));
        }
        return ShoppingListBuilder.build(inputs);
    }
}
