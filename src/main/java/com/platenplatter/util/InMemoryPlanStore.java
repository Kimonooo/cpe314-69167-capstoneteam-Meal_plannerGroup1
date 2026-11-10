package com.platenplatter.util;

import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * A PlanStore that keeps everything in plain lists and maps, for the tests.
 * No database file is needed, and it follows the same rules as MealPlanDao:
 * one saved recipe per name, its ingredients saved once, its usual calories remembered.
 */
public class InMemoryPlanStore implements PlanStore {
    private final List<PlannedMeal> meals = new ArrayList<>();
    private final Map<String, SavedRecipe> recipes = new HashMap<>();             // by lower-case name
    private final Map<Long, List<IngredientAmount>> amounts = new HashMap<>();    // by saved_recipe_id
    private final AtomicLong nextId = new AtomicLong(1);

    @Override
    public synchronized long saveMeal(PlannedMeal meal, List<IngredientAmount> ingredients) {
        SavedRecipe recipe = saveRecipe(meal);
        long id = nextId.getAndIncrement();
        meals.add(meal.saved(id, 1, recipe));
        saveAmounts(recipe, ingredients);
        return id;
    }

    @Override
    public synchronized List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) {
        return meals.stream()
                .filter(m -> !m.getPlanDate().isBefore(from) && !m.getPlanDate().isAfter(to))
                .sorted(Comparator.comparing(PlannedMeal::getPlanDate))
                .toList();
    }

    @Override
    public synchronized List<PlannedMeal> allMeals() {
        return meals.stream().sorted(Comparator.comparing(PlannedMeal::getPlanDate)).toList();
    }

    @Override
    public synchronized Set<LocalDate> plannedDates() {
        return meals.stream().map(PlannedMeal::getPlanDate).collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public synchronized void deleteMeal(long plannedMealId) {
        meals.removeIf(m -> m.getPlannedMealId() == plannedMealId);
    }

    @Override
    public synchronized List<IngredientAmount> shoppingItems(LocalDate from, LocalDate to) {
        return mealsBetween(from, to).stream()
                .flatMap(m -> amounts.getOrDefault(m.getRecipe().getSavedRecipeId(), List.of()).stream())
                .toList();
    }

    @Override
    public synchronized void saveIngredients(String recipeName, List<IngredientAmount> found) {
        SavedRecipe recipe = recipes.get(key(recipeName));
        if (recipe != null) {
            saveAmounts(recipe, found);
        }
    }

    @Override
    public synchronized OptionalInt usualCalories(String recipeName) {
        SavedRecipe recipe = recipes.get(key(recipeName));
        return recipe == null || recipe.getUsualCalories() == 0 ? OptionalInt.empty() : OptionalInt.of(recipe.getUsualCalories());
    }

    /** One saved recipe per name; planning it again only updates its usual calories. */
    private SavedRecipe saveRecipe(PlannedMeal meal) {
        SavedRecipe old = recipes.get(key(meal.getRecipeName()));
        SavedRecipe given = meal.getRecipe();
        long id = old == null ? nextId.getAndIncrement() : old.getSavedRecipeId();
        SavedRecipe base = old == null ? given : old;
        int calories = meal.getCalories() > 0 ? meal.getCalories() : base.getUsualCalories();
        SavedRecipe saved = SavedRecipe.create(base.getMealType(), id, base.getRecipeName(), base.getCuisine(),
                base.getBasePrepMinutes(), calories, base.getMealDbId());
        recipes.put(key(meal.getRecipeName()), saved);
        return saved;
    }

    /** Adds each ingredient once per recipe (like the UNIQUE rule of ingredient_amounts). */
    private void saveAmounts(SavedRecipe recipe, List<IngredientAmount> found) {
        List<IngredientAmount> list = amounts.computeIfAbsent(recipe.getSavedRecipeId(), id -> new ArrayList<>());
        found.stream()
                .filter(a -> list.stream().noneMatch(b -> b.getIngredient().getIngredientName()
                        .equalsIgnoreCase(a.getIngredient().getIngredientName())))
                .map(a -> new IngredientAmount(nextId.getAndIncrement(), recipe.getSavedRecipeId(), a.getIngredient(), a.getMeasure()))
                .forEach(list::add);
    }

    private static String key(String recipeName) {
        return recipeName == null ? "" : recipeName.trim().toLowerCase(Locale.ROOT);
    }
}
