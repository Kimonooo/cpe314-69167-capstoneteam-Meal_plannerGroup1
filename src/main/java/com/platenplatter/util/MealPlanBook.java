package com.platenplatter.util;

import com.platenplatter.model.MealPlanEntry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Keeps planned meals grouped by date. A Map gives a fast "what is planned on this day?". */
public class MealPlanBook {
    private final Map<LocalDate, List<MealPlanEntry>> byDate = new TreeMap<>();

    public void add(MealPlanEntry entry) {
        byDate.computeIfAbsent(entry.getPlanDate(), date -> new ArrayList<>()).add(entry);
    }

    /** All meals planned on this date (an empty list if there are none). */
    public List<MealPlanEntry> getMealsOn(LocalDate date) {
        return Collections.unmodifiableList(byDate.getOrDefault(date, List.of()));
    }

    public boolean hasMeals(LocalDate date) {
        return byDate.containsKey(date);
    }
}
