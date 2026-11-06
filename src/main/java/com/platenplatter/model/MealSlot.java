package com.platenplatter.model;

/**
 * The meal of the day a dish is planned for: Breakfast, Lunch, Dinner or Snack.
 * Each slot has a calorie budget, its share of a 2,000-calorie day
 * (the reference day used on nutrition labels): 500 + 650 + 650 + 200 = 2,000.
 * HealthClassifier uses this budget to decide if a meal is Healthy, Moderate or Indulgent.
 * The database stores the enum name (BREAKFAST, LUNCH, DINNER, SNACK).
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

    /** The word shown on screen, for example "Breakfast". */
    public String getLabel() {
        return label;
    }

    /** This slot's share of a 2,000-calorie day, in kcal. */
    public int getCalorieBudget() {
        return calorieBudget;
    }

    /**
     * Reads "Breakfast", "BREAKFAST" or "breakfast" (spaces around it are ignored).
     * @throws IllegalArgumentException for any other word
     */
    public static MealSlot fromText(String text) {
        if (text != null) {
            String t = text.trim();
            for (MealSlot slot : values()) {
                if (slot.name().equalsIgnoreCase(t) || slot.label.equalsIgnoreCase(t)) {
                    return slot;
                }
            }
        }
        throw new IllegalArgumentException("Unknown meal slot (use Breakfast, Lunch, Dinner or Snack): " + text);
    }

    /** Like fromText, but returns the fallback instead of throwing (used when reading old database rows). */
    public static MealSlot fromTextOr(String text, MealSlot fallback) {
        try {
            return fromText(text);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    /**
     * The slot a recipe goes to when nobody picked one (old rows, CSV files with 4 columns).
     * Breakfast recipes go to Breakfast, desserts and snacks to Snack, everything else to Dinner.
     */
    public static MealSlot defaultFor(String category) {
        if (category == null) {
            return DINNER;
        }
        String c = category.trim();
        if (c.equalsIgnoreCase("Breakfast")) {
            return BREAKFAST;
        }
        if (c.equalsIgnoreCase("Dessert") || c.equalsIgnoreCase("Snack")) {
            return SNACK;
        }
        return DINNER;
    }

    @Override
    public String toString() {
        return label; // so a ComboBox<MealSlot> shows "Breakfast", not "BREAKFAST"
    }
}
