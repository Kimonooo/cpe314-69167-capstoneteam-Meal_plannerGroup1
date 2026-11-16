package com.platenplatter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.api.FakeSender;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.InMemoryPlanStore;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.service.ShoppingList;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShoppingListControllerTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    private final InMemoryPlanStore store = new InMemoryPlanStore();

    private static PlannedMeal meal(String name) {
        return new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, name, "", 20, 0, ""), DAY,
                MealSlot.DINNER, 30, 0, HealthyLevel.MODERATE);
    }

    @Test
    void oneRecipeOrAllRecipes() throws Exception {
        store.saveMeal(meal("Adobo"), List.of(new IngredientAmount("garlic", "3 cloves")));
        store.saveMeal(meal("Sinigang"), List.of(new IngredientAmount("tamarind", "1 pack")));
        ShoppingListController c = new ShoppingListController(store,
                new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.offline()));
        ShoppingListController.ListData data = c.load(DAY, DAY);
        List<ShoppingList.RecipeChoice> choices = c.choices(data);
        assertEquals(2, choices.size());
        assertEquals(1, ShoppingList.count(c.build(data, choices.get(0)).groups()));
        ShoppingListController.ShownList all = c.build(data, ShoppingList.ALL_RECIPES);
        assertEquals(2, ShoppingList.count(all.groups()));
        assertTrue(all.summary().startsWith("2 ingredients for all 2 planned meals"));
        assertTrue(c.asText(null, all).startsWith("Shopping list: All planned recipes"));
    }

    @Test
    void anEmptyPlanGivesAnEmptyList() throws Exception {
        ShoppingListController c = new ShoppingListController(store,
                new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.offline()));
        assertTrue(c.build(c.load(DAY, DAY), null).isEmpty());
    }

    @Test
    void aMissingRecipeIsFoundOnlyByItsExactName() throws Exception {
        store.saveMeal(meal("Spicy Arrabiata Penne"), List.of()); // imported without ingredients
        store.saveMeal(meal("Arrabiata"), List.of());                       // not the exact name
        ShoppingListController c = new ShoppingListController(store,
                new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.file("search_arrabiata.json")));
        assertEquals(1, c.lookUpMissing(List.of("Spicy Arrabiata Penne", "Arrabiata")));
        assertEquals(8, store.shoppingItems(DAY, DAY).size());
    }
}
