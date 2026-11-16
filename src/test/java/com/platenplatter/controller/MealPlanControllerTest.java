package com.platenplatter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.db.InMemoryPlanStore;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.util.CookingQueue;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The Meal Plan page's queue, stack and recursion, on a store with no database. */
class MealPlanControllerTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    private final InMemoryPlanStore store = new InMemoryPlanStore();
    private final MealPlanController controller = new MealPlanController(store);

    private static PlannedMeal meal(String name, MealSlot slot, int prep, int calories) {
        return new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, name, "", 20, calories,
                ""), DAY,
                slot, prep, calories, HealthyLevel.HEALTHY);
    }

    private void plan(PlannedMeal... meals) throws Exception {
        for (PlannedMeal meal : meals) {
            store.saveMeal(meal, List.of());
        }
    }

    @Test
    void theCookingQueueStartsWithBreakfastWhateverOrderTheMealsWerePlanned() throws Exception {
        plan(meal("Sinigang", MealSlot.DINNER, 60, 700), meal("Champorado", MealSlot.BREAKFAST, 20,
                400),
                meal("Pancit", MealSlot.LUNCH, 30, 600));
        CookingQueue queue = controller.day(DAY).cookingOrder();
        assertEquals("Champorado", queue.next().getRecipeName());     // first in, first out
        assertEquals("Pancit", queue.next().getRecipeName());
        assertEquals("Sinigang", queue.next().getRecipeName());
        assertNull(queue.next());                                     // empty: null, not a crash
    }

    @Test
    void undoPutsBackTheLastRemovedMealFirst() throws Exception {
        plan(meal("Adobo", MealSlot.LUNCH, 45, 620), meal("Leche Flan", MealSlot.SNACK, 50, 350));
        List<PlannedMeal> planned = store.mealsOn(DAY);
        controller.remove(planned.get(0));
        controller.remove(planned.get(1));
        assertTrue(store.mealsOn(DAY).isEmpty());
        assertEquals(planned.get(1).getRecipeName(), controller.undoName());
        // LIFO
        assertEquals(planned.get(1).getRecipeName(), controller.undoRemove().getRecipeName());
        assertEquals(planned.get(0).getRecipeName(), controller.undoRemove().getRecipeName());
        assertEquals(2, store.mealsOn(DAY).size());
        assertFalse(controller.canUndo());
    }

    @Test
    void undoWithNothingRemovedDoesNothing() throws Exception {
        assertNull(controller.undoRemove());
        assertEquals("", controller.undoName());
    }

    @Test
    void theDayTotalIsAddedUpThroughTheSlotGroups() throws Exception {
        plan(meal("Champorado", MealSlot.BREAKFAST, 20, 400), meal("Pancit", MealSlot.LUNCH, 30,
                600),
                meal("Lumpia", MealSlot.LUNCH, 25, 300));
        MealPlanController.DayTotals totals = controller.day(DAY).totals();
        assertEquals(75, totals.minutes()); // the planned minutes, not the recipe's
        assertEquals(1300, totals.calories());
        assertEquals(2, totals.slots().size());                       // Breakfast and Lunch only
        MealPlanController.SlotTotal lunch = totals.slots().get(1);
        assertEquals(MealSlot.LUNCH, lunch.slot());
        assertEquals(2, lunch.meals());
        assertEquals(900, lunch.calories());
    }

    @Test
    void anEmptyDayHasZeroTotals() {
        MealPlanController.DayTotals totals = MealPlanController.dayTotals(List.of());
        assertEquals(0, totals.minutes());
        assertEquals(0, totals.calories());
        assertTrue(totals.slots().isEmpty());
    }

    @Test
    void theMonthIsColoredByTheMostCommonLevel() throws Exception {
        plan(meal("Adobo", MealSlot.LUNCH, 45, 620));
        assertEquals(HealthyLevel.HEALTHY, controller.monthLevels(YearMonth.from(DAY)).get(DAY));
        assertTrue(controller.monthLevels(YearMonth.of(2026, 12)).isEmpty());
    }
}
