package com.platenplatter.util;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;

/**
 * Turns one line of a meal-history CSV into a PlannedMeal, or rejects it with a reason.
 *
 *   date,recipe_name,category,prep_minutes,meal_slot,calories,healthy_level
 *   2026-11-03,Chicken Adobo,MainCourse,45,Dinner,620,Healthy
 *
 * The first 4 columns are required; the last 3 are optional. Without them the meal gets the slot of
 * its type and a Healthy Level worked out from the calories. A bad line throws IllegalArgumentException,
 * which the import counts as skipped.
 */
public final class CsvRecipeParser {
    public static final int REQUIRED_FIELDS = 4;
    public static final int MAX_FIELDS = 7;

    /** Splits on commas that are outside "quotes". */
    private static final String COMMA_OUTSIDE_QUOTES = ",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)";

    private CsvRecipeParser() { }

    public static PlannedMeal parse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty line");
        }
        List<String> fields = splitCsv(line);
        if (fields.size() < REQUIRED_FIELDS || fields.size() > MAX_FIELDS) {
            throw new IllegalArgumentException("Expected 4 to 7 fields but found " + fields.size());
        }
        LocalDate date = date(fields.get(0));
        String name = fields.get(1);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Recipe name is empty");
        }
        String category = fields.get(2).isEmpty() ? "MainCourse" : fields.get(2);
        int prep = number(fields.get(3), "Prep minutes");
        String slotText = optional(fields, 4);
        MealSlot slot = slotText.isEmpty() ? MealSlot.defaultFor(category) : MealSlot.fromText(slotText);
        String caloriesText = optional(fields, 5);
        int calories = caloriesText.isEmpty() ? 0 : number(caloriesText, "Calories");
        String levelText = optional(fields, 6);
        HealthyLevel level = levelText.isEmpty()
                ? HealthClassifier.classify(calories, slot, category)
                : HealthyLevel.fromText(levelText);
        SavedRecipe recipe = SavedRecipe.create(CategoryMapper.toMealType(category), 0, name, "", prep, calories, "");
        return new PlannedMeal(0, 0, recipe, date, slot, prep, calories, level);
    }

    /** The fields of one line, trimmed, with surrounding quotes removed ("" inside quotes is one "). */
    static List<String> splitCsv(String line) {
        return Arrays.stream(line.split(COMMA_OUTSIDE_QUOTES, -1))
                .map(String::trim)
                .map(field -> field.length() >= 2 && field.startsWith("\"") && field.endsWith("\"")
                        ? field.substring(1, field.length() - 1).replace("\"\"", "\"").trim()
                        : field)
                .toList();
    }

    private static LocalDate date(String text) {
        try {
            return LocalDate.parse(text);       // yyyy-MM-dd; 2026-02-30 is rejected too
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Bad date (use yyyy-MM-dd): " + text);
        }
    }

    private static int number(String text, String what) {
        int value;
        try {
            value = Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(what + " is not a number: " + text);
        }
        if (value < 0) {
            throw new IllegalArgumentException(what + " cannot be negative: " + value);
        }
        return value;
    }

    private static String optional(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index) : "";
    }
}
