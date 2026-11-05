package com.platenplatter.util;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

/**
 * Everything the screens need from "the meal plan", in one place.
 * The real implementation is MealPlanHistoryDao (SQLite). InMemoryPlanStore is a fake used for
 * previews and tests. Screens only know THIS interface, so they never touch SQL.
 * All methods may be slow, so screens call them from a background Task.
 */
public interface PlanStore {
    /** Adds one meal chosen on the Recipes tab, together with its ingredient list. */
    void addMeal(ImportRow row, List<String> ingredients) throws Exception;

    /** Adds one meal from a CSV import (no ingredient list). */
    void addImported(ImportRow row) throws Exception;

    List<PlannedMeal> mealsOn(LocalDate date) throws Exception;

    /** All meals from one date to another, both days included. */
    List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) throws Exception;

    /** Every day that has at least one planned meal (used for the calendar and the streak). */
    Set<LocalDate> plannedDates() throws Exception;

    void deleteMeal(long id) throws Exception;

    /** The saved ingredient list of a recipe (empty if we never saved one). */
    List<String> ingredientsFor(String recipeName) throws Exception;

    void saveIngredients(String recipeName, List<String> ingredients) throws Exception;

    /** The category of every planned meal (input of the data-mining step). */
    List<String> categories() throws Exception;

    List<Integer> prepMinutes() throws Exception;
}
