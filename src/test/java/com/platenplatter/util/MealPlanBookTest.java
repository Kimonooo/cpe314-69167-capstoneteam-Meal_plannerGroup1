package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.MealPlanEntry;
import com.platenplatter.model.Recipe;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MealPlanBookTest {

    @Test
    void mealsAreFoundOnTheDateTheyWerePlanned() {
        MealPlanBook book = new MealPlanBook();
        Recipe oats = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        LocalDate day = LocalDate.of(2026, 10, 3);
        book.add(new MealPlanEntry(1, day, "Breakfast", oats));
        book.add(new MealPlanEntry(2, day, "Snack", oats));

        assertTrue(book.hasMeals(day));
        assertEquals(2, book.getMealsOn(day).size());
    }

    @Test
    void aDateWithNoMealsGivesAnEmptyList() {
        // edge case: asking about a day that has nothing planned
        MealPlanBook book = new MealPlanBook();
        LocalDate emptyDay = LocalDate.of(2026, 10, 4);

        assertFalse(book.hasMeals(emptyDay));
        assertTrue(book.getMealsOn(emptyDay).isEmpty());
    }
}
