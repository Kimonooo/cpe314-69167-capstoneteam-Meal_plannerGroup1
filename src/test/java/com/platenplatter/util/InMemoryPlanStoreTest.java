package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryPlanStoreTest {

    private static ImportRow row(String date, String name, String cat, int min) {
        return new ImportRow(date, name, cat, min);
    }

    @Test
    void mealsComeBackByDateAndRange() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addImported(row("2026-11-03", "Tapsilog", "Breakfast", 20));
        s.addImported(row("2026-11-05", "Leche Flan", "Dessert", 50));
        s.addImported(row("2026-11-09", "Chicken Adobo", "MainCourse", 45));
        assertEquals(1, s.mealsOn(LocalDate.of(2026, 11, 5)).size());
        assertEquals(2, s.mealsBetween(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5)).size());
        assertTrue(s.mealsOn(LocalDate.of(2030, 1, 1)).isEmpty());
    }

    @Test
    void rangeIncludesBothEndDays() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addImported(row("2026-11-03", "A", "Dessert", 5));
        s.addImported(row("2026-11-04", "B", "Dessert", 5));
        assertEquals(2, s.mealsBetween(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4)).size());
    }

    @Test
    void deleteRemovesOnlyThatMeal() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addImported(row("2026-11-03", "A", "Dessert", 5));
        s.addImported(row("2026-11-03", "B", "Dessert", 5));
        long firstId = s.mealsOn(LocalDate.of(2026, 11, 3)).get(0).getId();
        s.deleteMeal(firstId);
        List<PlannedMeal> left = s.mealsOn(LocalDate.of(2026, 11, 3));
        assertEquals(1, left.size());
        assertEquals("B", left.get(0).getRecipeName());
    }

    @Test
    void ingredientsAreSavedIgnoringCase() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addMeal(row("2026-11-03", "Chicken Adobo", "MainCourse", 45), List.of("Chicken", "Garlic"));
        assertEquals(List.of("Chicken", "Garlic"), s.ingredientsFor("chicken adobo"));
        assertTrue(s.ingredientsFor("Unknown dish").isEmpty());
    }

    @Test
    void allMealsComeBackByDateThenSlot() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addImported(row("2026-11-04", "Leche Flan", "Dessert", 50));   // Snack
        s.addImported(row("2026-11-04", "Tapsilog", "Breakfast", 20));   // Breakfast
        s.addImported(row("2026-11-03", "Chicken Adobo", "MainCourse", 45));
        List<PlannedMeal> all = s.allMeals();
        assertEquals(3, all.size());
        assertEquals("Chicken Adobo", all.get(0).getRecipeName());
        assertEquals("Tapsilog", all.get(1).getRecipeName());
        assertEquals("Leche Flan", all.get(2).getRecipeName());
    }

    @Test
    void plannedDatesHasNoDuplicates() {
        InMemoryPlanStore s = new InMemoryPlanStore();
        s.addImported(row("2026-11-03", "A", "Dessert", 5));
        s.addImported(row("2026-11-03", "B", "Dessert", 5));
        assertEquals(1, s.plannedDates().size());
    }
}
