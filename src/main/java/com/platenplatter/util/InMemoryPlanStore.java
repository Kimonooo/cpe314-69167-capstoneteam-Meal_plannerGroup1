package com.platenplatter.util;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A PlanStore that keeps everything in memory. Used for previews, tests and screenshots. */
public class InMemoryPlanStore implements PlanStore {
    /** Same order as the database: by date, then Breakfast, Lunch, Dinner, Snack, then oldest first. */
    private static final Comparator<PlannedMeal> PLAN_ORDER = Comparator
            .comparing(PlannedMeal::getDate)
            .thenComparing(PlannedMeal::getMealSlot)
            .thenComparingLong(PlannedMeal::getId);

    private final List<PlannedMeal> meals = new ArrayList<>();
    private final Map<String, List<String>> ingredients = new LinkedHashMap<>();
    private long nextId = 1;

    @Override
    public synchronized void addMeal(ImportRow row, List<String> ingredientList) {
        addImported(row);
        if (ingredientList != null && !ingredientList.isEmpty()) {
            saveIngredients(row.getRecipeName(), ingredientList);
        }
    }

    @Override
    public synchronized void addImported(ImportRow row) {
        meals.add(new PlannedMeal(nextId++, row.getDate(), row.getRecipeName(), row.getCategory(),
                row.getPrepMinutes(), row.getMealSlot(), row.getCalories(), row.getHealthyLevel()));
    }

    @Override
    public synchronized List<PlannedMeal> mealsOn(LocalDate date) {
        return mealsBetween(date, date);
    }

    @Override
    public synchronized List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) {
        List<PlannedMeal> out = new ArrayList<>();
        for (PlannedMeal m : meals) {
            LocalDate d = m.getLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)) {
                out.add(m);
            }
        }
        out.sort(PLAN_ORDER);
        return out;
    }

    @Override
    public synchronized List<PlannedMeal> allMeals() {
        List<PlannedMeal> out = new ArrayList<>(meals);
        out.sort(PLAN_ORDER);
        return out;
    }

    @Override
    public synchronized Set<LocalDate> plannedDates() {
        Set<LocalDate> out = new HashSet<>();
        for (PlannedMeal m : meals) {
            out.add(m.getLocalDate());
        }
        return out;
    }

    @Override
    public synchronized void deleteMeal(long id) {
        meals.removeIf(m -> m.getId() == id);
    }

    @Override
    public synchronized List<String> ingredientsFor(String recipeName) {
        List<String> found = ingredients.get(recipeName.toLowerCase());
        return found == null ? new ArrayList<>() : new ArrayList<>(found);
    }

    @Override
    public synchronized void saveIngredients(String recipeName, List<String> list) {
        ingredients.put(recipeName.toLowerCase(), new ArrayList<>(list));
    }

    @Override
    public synchronized List<String> categories() {
        List<String> out = new ArrayList<>();
        for (PlannedMeal m : meals) {
            out.add(m.getCategory());
        }
        return out;
    }

    @Override
    public synchronized List<Integer> prepMinutes() {
        List<Integer> out = new ArrayList<>();
        for (PlannedMeal m : meals) {
            out.add(m.getPrepMinutes());
        }
        return out;
    }
}
