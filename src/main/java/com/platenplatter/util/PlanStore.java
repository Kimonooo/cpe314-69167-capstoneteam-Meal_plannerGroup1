package com.platenplatter.util;

import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;

/**
 * What a screen may ask of the saved meals. The screens only know this interface, so the same
 * screen runs on the SQLite database (MealPlanDao) or on plain lists in the tests (InMemoryPlanStore).
 */
public interface PlanStore {

    /** Saves one meal and its recipe's ingredient amounts (an empty list is fine). Returns the new planned_meal_id. */
    long saveMeal(PlannedMeal meal, List<IngredientAmount> ingredients) throws Exception;

    /** The meals from one day to another, both days included, by date. */
    List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) throws Exception;

    /** The meals of one day. A default method: it just asks mealsBetween for that one day. */
    default List<PlannedMeal> mealsOn(LocalDate day) throws Exception {
        return mealsBetween(day, day);
    }

    List<PlannedMeal> allMeals() throws Exception;

    /** Every day that has at least one meal (for the streak and the week strip). */
    Set<LocalDate> plannedDates() throws Exception;

    void deleteMeal(long plannedMealId) throws Exception;

    /** The ingredient amounts of every meal planned in the date range (for the shopping list). */
    List<IngredientAmount> shoppingItems(LocalDate from, LocalDate to) throws Exception;

    /** Saves ingredients found online for a recipe that was planned without them. */
    void saveIngredients(String recipeName, List<IngredientAmount> amounts) throws Exception;

    /** The calories entered the last time this recipe was planned, if any. */
    OptionalInt usualCalories(String recipeName) throws Exception;
}
