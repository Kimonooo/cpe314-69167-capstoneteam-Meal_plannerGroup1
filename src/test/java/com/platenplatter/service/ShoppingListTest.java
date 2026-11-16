package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.Ingredient;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ShoppingListTest {

    private static IngredientAmount amount(long recipeId, String name, String measure) {
        return new IngredientAmount(0, recipeId, Ingredient.named(name), measure);
    }

    private static PlannedMeal meal(long recipeId, String name) {
        return new PlannedMeal(1, 1, SavedRecipe.create("MAIN_COURSE", recipeId, name, "", 30, 0, ""),
                LocalDate.of(2026, 11, 2), MealSlot.DINNER, 40, 0, HealthyLevel.MODERATE);
    }

    @Test
    void theSameIngredientFromTwoRecipesIsListedOnceWithBothMeasures() {
        Map<FoodGroup, Map<String, List<String>>> list = ShoppingList.build(List.of(
                amount(1, "Garlic", "3 cloves"), amount(2, "garlic", "1 head"), amount(2, "Chicken", "1 kg")));
        assertEquals(List.of("3 cloves", "1 head"), list.get(FoodGroup.FRUITS_VEGETABLES).get("garlic"));
        assertEquals(2, ShoppingList.count(list));
    }

    @Test
    void groupsFollowTheFoodGroupOrderAndItemsAreAToZ() {
        Map<FoodGroup, Map<String, List<String>>> list = ShoppingList.build(List.of(
                amount(1, "Soy sauce", ""), amount(1, "Onion", "1"), amount(1, "Chicken", "1 kg"), amount(1, "Garlic", "3 cloves")));
        assertEquals(List.of(FoodGroup.FRUITS_VEGETABLES, FoodGroup.MEAT_SEAFOOD, FoodGroup.SAUCES_CONDIMENTS),
                new ArrayList<>(list.keySet()));
        assertEquals(List.of("garlic", "onion"), new ArrayList<>(list.get(FoodGroup.FRUITS_VEGETABLES).keySet()));
    }

    @Test
    void recipesWithoutIngredientsAreReported() {
        List<PlannedMeal> meals = List.of(meal(1, "Chicken Adobo"), meal(2, "Leche Flan"), meal(2, "Leche Flan"), meal(3, "Bibingka"));
        List<IngredientAmount> items = List.of(amount(1, "Chicken", "1 kg"));
        assertEquals(List.of("Bibingka", "Leche Flan"), ShoppingList.missingRecipes(meals, items));
    }

    @Test
    void aLineLeavesOutBlankMeasures() {
        assertEquals("garlic (3 cloves, 1 head)", ShoppingList.line("garlic", List.of("3 cloves", "", "1 head")));
        assertEquals("salt", ShoppingList.line("salt", List.of("")));
    }

    @Test
    void theTextVersionHasAHeadingPerGroupAndATickBoxPerItem() {
        String text = ShoppingList.asText(ShoppingList.build(List.of(amount(1, "Garlic", "3 cloves"), amount(1, "Chicken", "1 kg"))));
        assertTrue(text.startsWith("Fruits & Vegetables\n  [ ] garlic (3 cloves)"));
        assertTrue(text.contains("Meat & Seafood\n  [ ] chicken (1 kg)"));
    }

    @Test
    void anEmptyPlanGivesAnEmptyList() {
        assertTrue(ShoppingList.build(List.of()).isEmpty());
        assertEquals("", ShoppingList.asText(ShoppingList.build(List.of())));
        assertTrue(ShoppingList.missingRecipes(List.of(), List.of()).isEmpty());
    }

    @Test
    void theRecipeChoicesListEachPlannedRecipeOnceAToZ() {
        List<ShoppingList.RecipeChoice> choices = ShoppingList.recipeChoices(
                List.of(meal(2, "Sinigang"), meal(1, "Adobo"), meal(2, "Sinigang")));
        assertEquals(List.of(new ShoppingList.RecipeChoice(1, "Adobo"), new ShoppingList.RecipeChoice(2, "Sinigang")), choices);
        assertEquals("Adobo", choices.get(0).toString());
        assertTrue(ShoppingList.recipeChoices(List.of()).isEmpty());
    }

    @Test
    void theListForOneRecipeHasOnlyThatRecipesIngredientsEachOnce() {
        IngredientAmount garlic = new IngredientAmount(10, 1, Ingredient.named("Garlic"), "3 cloves");
        IngredientAmount chicken = new IngredientAmount(11, 1, Ingredient.named("Chicken"), "1 kg");
        IngredientAmount tamarind = new IngredientAmount(12, 2, Ingredient.named("Tamarind"), "1 pack");
        // recipe 1 is planned on two days, so the database gives its amounts twice
        List<IngredientAmount> items = List.of(garlic, chicken, tamarind, garlic, chicken);
        Map<FoodGroup, Map<String, List<String>>> list = ShoppingList.build(ShoppingList.forRecipe(items, 1));
        assertEquals(2, ShoppingList.count(list));
        assertEquals(List.of("3 cloves"), list.get(FoodGroup.FRUITS_VEGETABLES).get("garlic"));
        assertEquals(1, ShoppingList.forRecipe(items, 2).size());
        assertTrue(ShoppingList.forRecipe(items, 99).isEmpty());
        // "All planned recipes" shows a recipe planned on two days once, too: no "garlic (3 cloves, 3 cloves)"
        Map<FoodGroup, Map<String, List<String>>> all = ShoppingList.build(ShoppingList.distinct(items));
        assertEquals(3, ShoppingList.count(all));
        assertEquals(List.of("3 cloves"), all.get(FoodGroup.FRUITS_VEGETABLES).get("garlic"));
        assertEquals(2, ShoppingList.mealsOf(List.of(meal(1, "Adobo"), meal(2, "Sinigang"), meal(1, "Adobo")), 1).size());
    }
}
