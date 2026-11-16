package com.platenplatter.controller;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.PlanStore;
import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.ShoppingList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The controller of the Shopping List page: it reads the planned meals and their ingredients,
 * builds the list for one recipe (or for all of them) with ShoppingList, and looks up recipes
 * that were planned without ingredients. The page only shows what this class returns.
 */
public class ShoppingListController {

    /** The meals planned in the chosen days and the ingredient amounts of their recipes. */
    public record ListData(List<PlannedMeal> meals, List<IngredientAmount> items) {
        public static final ListData EMPTY = new ListData(List.of(), List.of());
    }

    /** One list ready to show: the groups, the recipes with no ingredients yet, and a summary. */
    public record ShownList(Map<FoodGroup, Map<String, List<String>>> groups, List<String> missing,
                            String summary) {
        public boolean isEmpty() {
            return groups.isEmpty();
        }
    }

    private final PlanStore store;
    private final MealDbClient client;

    public ShoppingListController(PlanStore store, MealDbClient client) {
        this.store = store;
        this.client = client;
    }

    /** Reads the meals and their ingredients from one day to another (call it in a Task). */
    public ListData load(LocalDate from, LocalDate to) throws SQLException {
        return new ListData(store.mealsBetween(from, to), store.shoppingItems(from, to));
    }

    /** The recipes to pick from: each planned recipe once, then "All planned recipes". */
    public List<ShoppingList.RecipeChoice> choices(ListData data) {
        return ShoppingList.recipeChoices(data.meals());
    }

    /** The list for one recipe, or for all of them when the choice is null or ALL_RECIPES. */
    public ShownList build(ListData data, ShoppingList.RecipeChoice choice) {
        if (choice == null || ShoppingList.ALL_RECIPES.equals(choice)) {
            Map<FoodGroup, Map<String, List<String>>> groups =
                    ShoppingList.build(ShoppingList.distinct(data.items()));
            return new ShownList(groups, ShoppingList.missingRecipes(data.meals(), data.items()),
                    ShoppingList.count(groups) + " ingredients for all " + data.meals().size()
                            + " planned meals.");
        }
        List<IngredientAmount> items = ShoppingList.forRecipe(data.items(), choice.savedRecipeId());
        List<PlannedMeal> meals = ShoppingList.mealsOf(data.meals(), choice.savedRecipeId());
        Map<FoodGroup, Map<String, List<String>>> groups = ShoppingList.build(items);
        return new ShownList(groups, ShoppingList.missingRecipes(meals, items),
                ShoppingList.count(groups) + " ingredients for " + choice.name() + " (planned "
                        + meals.size() + (meals.size() == 1 ? " time)." : " times)."));
    }

    /** The text that is copied or saved: a title line, then the list. */
    public String asText(ShoppingList.RecipeChoice choice, ShownList list) {
        String name = choice == null ? ShoppingList.ALL_RECIPES.name() : choice.name();
        return "Shopping list: " + name + "\n\n" + ShoppingList.asText(list.groups());
    }

    /** Writes the list to a text file the user picked. */
    public void save(Path file, String text) throws IOException {
        Files.writeString(file, text);
    }

    /**
     * Looks up each recipe that has no ingredients on TheMealDB; only an exact name match is
     * saved, so a different dish is never used. Returns how many were found (call it in a Task).
     */
    public int lookUpMissing(List<String> names) throws IOException, InterruptedException,
            SQLException {
        int found = 0;
        for (String name : names) {
            Optional<MealData> match = client.findExact(name);
            if (match.isPresent()) {
                store.saveIngredients(name, match.get().ingredients());
                found++;
            }
        }
        return found;
    }
}
