package com.platenplatter.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Builds a shopping list: merges the same ingredient from different meals into ONE line, counts how
 * many meals need it, and groups the lines by shop aisle (each aisle sorted A to Z).
 * Pure logic: no database and no window, so it is easy to test.
 */
public final class ShoppingListBuilder {
    private ShoppingListBuilder() { }

    private static final class Agg {
        final String key;
        final Aisle aisle;
        int meals;
        final TreeSet<String> recipes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        Agg(String key, Aisle aisle) {
            this.key = key;
            this.aisle = aisle;
        }
    }

    public static ShoppingList build(List<MealIngredients> meals) {
        Map<String, Agg> byKey = new LinkedHashMap<>();
        List<String> missing = new ArrayList<>();
        int mealCount = 0;

        for (MealIngredients meal : meals) {
            mealCount += meal.getTimesPlanned();
            TreeSet<String> keysOfThisRecipe = new TreeSet<>(); // an ingredient written twice counts once
            for (String raw : meal.getIngredients()) {
                String key = IngredientCategorizer.normalize(raw);
                if (!key.isEmpty()) {
                    keysOfThisRecipe.add(key);
                }
            }
            if (keysOfThisRecipe.isEmpty()) {
                missing.add(meal.getRecipeName());
                continue;
            }
            for (String key : keysOfThisRecipe) {
                Agg agg = byKey.computeIfAbsent(key, k -> new Agg(k, IngredientCategorizer.aisleFor(k)));
                agg.meals += meal.getTimesPlanned();
                agg.recipes.add(meal.getRecipeName());
            }
        }

        Map<Aisle, List<ShoppingList.Item>> byAisle = new EnumMap<>(Aisle.class);
        for (Agg agg : byKey.values()) {
            byAisle.computeIfAbsent(agg.aisle, a -> new ArrayList<>()).add(new ShoppingList.Item(
                    agg.key, IngredientCategorizer.display(agg.key), agg.aisle, agg.meals, new ArrayList<>(agg.recipes)));
        }
        List<ShoppingList.Section> sections = new ArrayList<>();
        for (Aisle aisle : Aisle.values()) {
            List<ShoppingList.Item> items = byAisle.get(aisle);
            if (items != null) {
                items.sort(Comparator.comparing(ShoppingList.Item::getName, String.CASE_INSENSITIVE_ORDER));
                sections.add(new ShoppingList.Section(aisle, items));
            }
        }
        missing.sort(String.CASE_INSENSITIVE_ORDER);
        return new ShoppingList(sections, missing, mealCount);
    }
}
