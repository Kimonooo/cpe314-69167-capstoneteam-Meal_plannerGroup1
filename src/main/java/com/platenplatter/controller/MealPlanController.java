package com.platenplatter.controller;

import com.platenplatter.db.PlanStore;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.service.RecipeCalculator;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.CookingQueue;
import com.platenplatter.util.UndoStack;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * The controller of the Meal Plan page. It is where the three data structures of the course are
 * used by a page:
 *   QUEUE     the day's meals in cooking order (CookingQueue): breakfast first, "Done, next";
 *   STACK     every removed meal is pushed on an UndoStack, and Undo puts back the last one first;
 *   RECURSION the day's totals: the day is a tree of recipes (day, then one group per meal slot,
 *             then the meals) and RecipeCalculator.totalOf adds it up recursively.
 * Call the methods that read or write from a background Task.
 */
public class MealPlanController {

    /** One meal slot's share of the day. */
    public record SlotTotal(MealSlot slot, int meals, int minutes, int calories) { }

    /** The whole day: minutes and calories of every meal, and each slot's part. */
    public record DayTotals(int minutes, int calories, List<SlotTotal> slots) { }

    /** Everything the day card shows. The queue is used on the JavaFX thread only. */
    public record DayPlan(LocalDate day, List<PlannedMeal> meals, CookingQueue cookingOrder,
                          DayTotals totals) { }

    private final PlanStore store;
    private final UndoStack<PlannedMeal> removed = new UndoStack<>();

    public MealPlanController(PlanStore store) {
        this.store = store;
    }

    /** The healthy level most meals of each day have, for coloring the month. */
    public Map<LocalDate, HealthyLevel> monthLevels(YearMonth month) throws SQLException {
        return StatisticsService.levelByDay(store.mealsBetween(month.atDay(1),
                month.atEndOfMonth()));
    }

    /** The meals of one day with their cooking order and their totals. */
    public DayPlan day(LocalDate day) throws SQLException {
        List<PlannedMeal> meals = store.mealsOn(day);
        return new DayPlan(day, meals, cookingOrder(meals), dayTotals(meals));
    }

    /** STACK: removes the meal and remembers it, so Undo can bring it back. */
    public synchronized void remove(PlannedMeal meal) throws SQLException {
        store.deleteMeal(meal.getPlannedMealId());
        removed.push(meal);
    }

    /** True when there is a removed meal to put back. */
    public synchronized boolean canUndo() {
        return !removed.isEmpty();
    }

    /** The name of the meal Undo would put back, or "" when there is none. */
    public synchronized String undoName() {
        PlannedMeal last = removed.peek();
        return last == null ? "" : last.getRecipeName();
    }

    /**
     * Puts back the most recently removed meal (last in, first out). The saved recipe and its
     * ingredient list were never deleted, so only the planned meal is saved again.
     * Returns that meal, or null when nothing was removed.
     */
    public synchronized PlannedMeal undoRemove() throws SQLException {
        PlannedMeal last = removed.peek();
        if (last == null) {
            return null;
        }
        store.saveMeal(last, List.of());
        removed.pop();                                   // taken off only after it was saved again
        return last;
    }

    /** QUEUE: the day's recipes in cooking order, breakfast first and snack last. */
    public static CookingQueue cookingOrder(List<PlannedMeal> meals) {
        CookingQueue queue = new CookingQueue();
        meals.stream()
                .sorted(Comparator.comparing(PlannedMeal::getMealSlot))
                .map(PlannedMeal::getRecipe)
                .forEach(queue::add);
        return queue;
    }

    /**
     * RECURSION: builds the day as a tree and adds it up with RecipeCalculator.totalOf.
     * The day node holds one group node per meal slot; each group holds a copy of every meal's
     * recipe whose base minutes and calories are the ones planned for that meal.
     */
    public static DayTotals dayTotals(List<PlannedMeal> meals) {
        SavedRecipe day = group("Whole day");
        Map<MealSlot, SavedRecipe> groups = new EnumMap<>(MealSlot.class);
        Map<MealSlot, Integer> counts = new EnumMap<>(MealSlot.class);
        meals.forEach(meal -> {
            SavedRecipe slotGroup = groups.computeIfAbsent(meal.getMealSlot(), slot -> {
                SavedRecipe made = group(slot.getLabel());
                day.addSubRecipe(made);
                return made;
            });
            slotGroup.addSubRecipe(asPlanned(meal));
            counts.merge(meal.getMealSlot(), 1, Integer::sum);
        });
        List<SlotTotal> slots = groups.entrySet().stream()
                .map(entry -> new SlotTotal(entry.getKey(), counts.get(entry.getKey()),
                        RecipeCalculator.totalOf(entry.getValue(), SavedRecipe::getBasePrepMinutes),
                        RecipeCalculator.totalCalories(entry.getValue())))
                .toList();
        return new DayTotals(RecipeCalculator.totalOf(day, SavedRecipe::getBasePrepMinutes),
                RecipeCalculator.totalCalories(day), slots);
    }

    /** A node of the tree that only holds other recipes: no minutes and no calories of its own. */
    private static SavedRecipe group(String name) {
        return SavedRecipe.create("MAIN_COURSE", 0, name, "", 0, 0, "");
    }

    /** A copy of the meal's recipe carrying the minutes and calories planned for this meal. */
    private static SavedRecipe asPlanned(PlannedMeal meal) {
        SavedRecipe recipe = meal.getRecipe();
        return SavedRecipe.create(recipe.getMealType(), meal.getPlannedMealId(),
                recipe.getRecipeName(),
                recipe.getCuisine(), meal.getPrepMinutes(), meal.getCalories(),
                        recipe.getMealDbId());
    }
}
