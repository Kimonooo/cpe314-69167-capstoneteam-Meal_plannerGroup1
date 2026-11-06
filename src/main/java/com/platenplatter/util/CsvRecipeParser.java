package com.platenplatter.util;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one CSV line into an ImportRow, or throws IllegalArgumentException
 * with a readable reason. Responsibility: parsing + validation only.
 *
 * Expected format:  date,recipe_name,category,prep_minutes[,meal_slot,calories,healthy_level]
 * The first 4 columns are required. The last 3 are optional (old files only have 4), and an
 * optional column may also be left empty:
 *   meal_slot     Breakfast, Lunch, Dinner or Snack   (empty = default for the category)
 *   calories      a whole number, 0 or more           (empty = 0 = not known)
 *   healthy_level Healthy, Moderate or Indulgent      (empty = worked out from the calories)
 */
public final class CsvRecipeParser {
    public static final int REQUIRED_FIELDS = 4;
    public static final int MAX_FIELDS = 7;

    private CsvRecipeParser() { }

    public static ImportRow parseLine(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty line");
        }
        List<String> fields = splitCsv(line);
        if (fields.size() < REQUIRED_FIELDS || fields.size() > MAX_FIELDS) {
            throw new IllegalArgumentException("Expected 4 to 7 fields but found " + fields.size());
        }
        String date = fields.get(0).trim();
        String name = fields.get(1).trim();
        String category = fields.get(2).trim();
        if (!date.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Bad date (use yyyy-MM-dd): " + date);
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Recipe name is empty");
        }
        int prep;
        try {
            prep = Integer.parseInt(fields.get(3).trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Prep minutes is not a number: " + fields.get(3));
        }
        if (prep < 0) {
            throw new IllegalArgumentException("Prep minutes cannot be negative: " + prep);
        }
        if (category.isEmpty()) {
            category = "Uncategorized";
        }

        String slotText = optional(fields, 4);
        MealSlot slot = slotText.isEmpty() ? null : MealSlot.fromText(slotText); // throws a readable reason

        String caloriesText = optional(fields, 5);
        int calories = 0;
        if (!caloriesText.isEmpty()) {
            try {
                calories = Integer.parseInt(caloriesText);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Calories is not a number: " + caloriesText);
            }
            if (calories < 0) {
                throw new IllegalArgumentException("Calories cannot be negative: " + calories);
            }
        }

        String levelText = optional(fields, 6);
        HealthyLevel level = levelText.isEmpty() ? null : HealthyLevel.fromText(levelText);

        return new ImportRow(date, name, category, prep, slot, calories, level);
    }

    /** The trimmed text of an optional column, or "" when the line does not have it. */
    private static String optional(List<String> fields, int index) {
        return index < fields.size() ? fields.get(index).trim() : "";
    }

    /** Splits on commas but keeps commas that are inside "quotes". */
    static List<String> splitCsv(String line) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cur.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                out.add(cur.toString());
                cur.setLength(0);
            } else {
                cur.append(c);
            }
        }
        out.add(cur.toString());
        return out;
    }
}
