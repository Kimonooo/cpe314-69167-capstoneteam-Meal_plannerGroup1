package com.platenplatter.util;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns one CSV line into an ImportRow, or throws IllegalArgumentException
 * with a readable reason. Responsibility: parsing + validation only.
 * Expected format: date,recipe_name,category,prep_minutes
 */
public final class CsvRecipeParser {
    private CsvRecipeParser() { }

    public static ImportRow parseLine(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Empty line");
        }
        List<String> fields = splitCsv(line);
        if (fields.size() != 4) {
            throw new IllegalArgumentException("Expected 4 fields but found " + fields.size());
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
        return new ImportRow(date, name, category.isEmpty() ? "Uncategorized" : category, prep);
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
