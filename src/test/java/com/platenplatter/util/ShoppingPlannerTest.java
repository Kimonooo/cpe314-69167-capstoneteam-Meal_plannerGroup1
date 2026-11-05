package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ShoppingPlannerTest {

    private static ImportRow row(String date, String name) {
        return new ImportRow(date, name, "MainCourse", 30);
    }

    @Test
    void usesSavedIngredientsFirstAndOnlyMealsInTheRange() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.addMeal(row("2026-11-04", "Teriyaki Chicken"), List.of("Chicken", "Teriyaki sauce", "Rice"));
        store.addMeal(row("2026-11-20", "Far Away Meal"), List.of("Truffle"));
        ShoppingList list = ShoppingListPlannerHelper.plan(store, "2026-11-01", "2026-11-10");
        assertEquals(3, list.itemCount());
        assertEquals(1, list.getMealCount());
    }

    @Test
    void fallsBackToTheBuiltInIngredientsOfSampleDishes() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.addImported(row("2026-11-04", "Chicken Adobo"));
        ShoppingList list = ShoppingListPlannerHelper.plan(store, "2026-11-04", "2026-11-04");
        assertFalse(list.isEmpty());
        assertTrue(list.getMissingRecipes().isEmpty());
    }

    @Test
    void unknownImportedDishesAreListedAsMissing() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.addImported(row("2026-11-04", "Grandma's Secret Dish"));
        ShoppingList list = ShoppingListPlannerHelper.plan(store, "2026-11-04", "2026-11-04");
        assertTrue(list.isEmpty());
        assertEquals(List.of("Grandma's Secret Dish"), list.getMissingRecipes());
    }

    @Test
    void theSameDishOnTwoDaysCountsTwiceAndMatchesIgnoringCapitals() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        store.addImported(row("2026-11-04", "Chicken Adobo"));
        store.addImported(row("2026-11-06", "chicken adobo"));
        ShoppingList list = ShoppingListPlannerHelper.plan(store, "2026-11-01", "2026-11-30");
        assertEquals(2, list.getMealCount());
        ShoppingList.Item garlic = null;
        for (ShoppingList.Section s : list.getSections()) {
            for (ShoppingList.Item i : s.getItems()) {
                if (i.getName().equals("Garlic")) {
                    garlic = i;
                }
            }
        }
        assertNotNull(garlic);
        assertEquals(2, garlic.getMealCount());
    }

    @Test
    void anEmptyRangeGivesAnEmptyList() throws Exception {
        ShoppingList list = ShoppingPlanner.plan(new InMemoryPlanStore(), LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 7));
        assertTrue(list.isEmpty());
        assertEquals(0, list.getMealCount());
    }

    /** Small helper so the tests can write dates as text. */
    private static final class ShoppingListPlannerHelper {
        static ShoppingList plan(PlanStore store, String from, String to) throws Exception {
            return ShoppingPlanner.plan(store, LocalDate.parse(from), LocalDate.parse(to));
        }
    }
}
