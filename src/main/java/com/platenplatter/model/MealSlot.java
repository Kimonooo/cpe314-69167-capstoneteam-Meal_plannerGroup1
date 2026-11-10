package com.platenplatter.model;

import java.util.Arrays;

/**
 * The four meals of a day. Each one has a calorie budget, its share of a 2,000-calorie day
 * (the day used on food labels): Breakfast 500 + Lunch 650 + Dinner 650 + Snack 200 = 2,000.
 * The database stores name(), for example LUNCH.
 */
public enum MealSlot {
    BREAKFAST("Breakfast", 500),
    LUNCH("Lunch", 650),
    DINNER("Dinner", 650),
    SNACK("Snack", 200);

    private final String label;
    private final int calorieBudget;

    MealSlot(String label, int calorieBudget) {
        this.label = label;
        this.calorieBudget = calorieBudget;
    }

    public String getLabel() {
        return label;
    }

    public int getCalorieBudget() {
        return calorieBudget;
    }

    /** Reads "LUNCH", "Lunch" or "lunch". Anything else throws, with a reason the import can show. */
    public static MealSlot fromText(String text) {
        String clean = text == null ? "" : text.trim();
        return Arrays.stream(values())
                .filter(slot -> slot.name().equalsIgnoreCase(clean) || slot.label.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown meal slot (use Breakfast, Lunch, Dinner or Snack): " + text));
    }

    /** Like fromText, but gives the fallback instead of throwing. */
    public static MealSlot fromTextOr(String text, MealSlot fallback) {
        String clean = text == null ? "" : text.trim();
        return Arrays.stream(values())
                .filter(slot -> slot.name().equalsIgnoreCase(clean) || slot.label.equalsIgnoreCase(clean))
                .findFirst()
                .orElse(fallback);
    }

    /**
     * The usual slot for a recipe type or meal type: breakfast recipes are Breakfast,
     * desserts are a Snack, everything else is Dinner.
     */
    public static MealSlot defaultFor(String type) {
        String clean = type == null ? "" : type.trim();
        if (clean.equalsIgnoreCase("Breakfast")) {
            return BREAKFAST;
        }
        if (clean.equalsIgnoreCase("Dessert") || clean.equalsIgnoreCase("Snack")) {
            return SNACK;
        }
        return DINNER;
    }

    @Override
    public String toString() {
        return label; // so a ComboBox<MealSlot> shows "Breakfast", not "BREAKFAST"
    }
}
