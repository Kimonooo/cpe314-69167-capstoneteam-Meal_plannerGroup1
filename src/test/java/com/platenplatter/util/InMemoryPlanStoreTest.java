package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class InMemoryPlanStoreTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private static PlannedMeal meal(String name, LocalDate date, int calories) {
        return new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, name, "", 30, 0, ""), date,
                MealSlot.DINNER, 40, calories, HealthyLevel.MODERATE);
    }

    @Test
    void aRangeIncludesBothEndDays() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.saveMeal(meal("A", MONDAY, 0), List.of());
        store.saveMeal(meal("B", MONDAY.plusDays(6), 0), List.of());
        store.saveMeal(meal("C", MONDAY.plusDays(7), 0), List.of());
        assertEquals(2, store.mealsBetween(MONDAY, MONDAY.plusDays(6)).size());
        assertEquals(1, store.mealsOn(MONDAY).size());     // the default method of PlanStore
    }

    @Test
    void plannedDatesHaveNoDuplicates() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.saveMeal(meal("A", MONDAY, 0), List.of());
        store.saveMeal(meal("B", MONDAY, 0), List.of());
        assertEquals(1, store.plannedDates().size());
    }

    @Test
    void savedMealsGetIdsAndCanBeDeleted() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        long id = store.saveMeal(meal("A", MONDAY, 0), List.of());
        assertTrue(id > 0);
        assertEquals(id, store.allMeals().get(0).getPlannedMealId());
        store.deleteMeal(id);
        assertTrue(store.allMeals().isEmpty());
    }

    @Test
    void theSameRecipeIsSavedOnceAndRemembersItsCalories() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.saveMeal(meal("Adobo", MONDAY, 620), List.of());
        store.saveMeal(meal("adobo", MONDAY.plusDays(1), 0), List.of());   // 0 = not known: keeps 620
        assertEquals(OptionalInt.of(620), store.usualCalories("ADOBO"));
        assertEquals(store.allMeals().get(0).getRecipe().getSavedRecipeId(),
                store.allMeals().get(1).getRecipe().getSavedRecipeId());
        assertFalse(store.usualCalories("Never planned").isPresent());
    }

    @Test
    void ingredientsAreSavedOncePerRecipeAndListedPerMeal() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        List<IngredientAmount> garlic = List.of(new IngredientAmount("garlic", "3 cloves"), new IngredientAmount("Garlic", "again"));
        store.saveMeal(meal("Adobo", MONDAY, 0), garlic);
        store.saveMeal(meal("Adobo", MONDAY.plusDays(1), 0), garlic);
        assertEquals(2, store.shoppingItems(MONDAY, MONDAY.plusDays(6)).size());    // 1 amount x 2 meals
        assertEquals(1, store.shoppingItems(MONDAY, MONDAY).size());
    }

    @Test
    void ingredientsFoundOnlineAreAddedToAnExistingRecipeOnly() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.saveMeal(meal("Tinola", MONDAY, 0), List.of());
        store.saveIngredients("tinola", List.of(new IngredientAmount("chicken", "1 whole")));
        store.saveIngredients("Unknown dish", List.of(new IngredientAmount("salt", "1 tsp")));
        assertEquals(1, store.shoppingItems(MONDAY, MONDAY).size());
    }
}
