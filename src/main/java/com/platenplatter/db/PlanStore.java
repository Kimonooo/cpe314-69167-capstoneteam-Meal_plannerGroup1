package com.platenplatter.db;

import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.IntPredicate;

/**
 * What the rest of the app may ask of the saved meals. The controllers only know this interface,
 * so the same code runs on the SQLite database (MealPlanDao) or on plain lists in the tests
 * (InMemoryPlanStore).
 */
public interface PlanStore {

    /**
     * How a batch save ended: the meals saved, and the ones skipped because they were planned
     * already.
     */
    record BatchResult(int saved, int duplicates) { }

    /**
     * Saves one meal and its recipe's ingredient amounts (an empty list is fine).
     * Returns the new planned_meal_id.
     */
    long saveMeal(PlannedMeal meal, List<IngredientAmount> ingredients) throws SQLException;

    /**
     * Saves many meals (a CSV import) as ONE transaction: all of them are saved, or none.
     * A meal that is already planned (same day, same meal slot, same recipe name) is skipped and
     * counted as a duplicate, so importing the same file twice adds nothing the second time.
     * After each meal, keepGoing is asked with the number of meals done so far; when it answers
     * false (the user pressed Cancel), everything is rolled back and a
     * java.util.concurrent.CancellationException is thrown.
     */
    BatchResult saveMeals(List<PlannedMeal> meals, IntPredicate keepGoing) throws SQLException;

    /** The meals from one day to another, both days included, by date. */
    List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) throws SQLException;

    /** The meals of one day. A default method: it just asks mealsBetween for that one day. */
    default List<PlannedMeal> mealsOn(LocalDate day) throws SQLException {
        return mealsBetween(day, day);
    }

    List<PlannedMeal> allMeals() throws SQLException;

    /** Every day that has at least one meal (for the streak and the week strip). */
    Set<LocalDate> plannedDates() throws SQLException;

    void deleteMeal(long plannedMealId) throws SQLException;

    /** The ingredient amounts of every meal planned in the date range (for the shopping list). */
    List<IngredientAmount> shoppingItems(LocalDate from, LocalDate to) throws SQLException;

    /** Saves ingredients found online for a recipe that was planned without them. */
    void saveIngredients(String recipeName, List<IngredientAmount> amounts) throws SQLException;

    /** The calories entered the last time this recipe was planned, if any. */
    OptionalInt usualCalories(String recipeName) throws SQLException;
}
