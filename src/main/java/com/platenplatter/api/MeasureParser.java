package com.platenplatter.api;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a TheMealDB measure such as "200g", "1 1/2 cups", "1/2 tsp" or "2 large" into a number
 * and a unit. TheMealDB gives the measure as free text and sometimes gives none at all; a blank
 * measure is shown as "amount not given" so the user knows it is missing, not zero.
 */
public final class MeasureParser {
    /** A whole number, a decimal, a fraction or a mixed number at the start, then the rest. */
    private static final Pattern AMOUNT = Pattern.compile(
            "^\\s*(\\d+\\s+\\d+/\\d+|\\d+/\\d+|\\d+(?:[.,]\\d+)?)\\s*(.*)$");

    /** The text shown when TheMealDB gives no measure. */
    public static final String NOT_GIVEN = "amount not given";

    /** amount is 0 when the text has no number ("pinch", "to taste" or blank). */
    public record Measure(double amount, String unit, String text) {

        public boolean hasAmount() {
            return amount > 0;
        }

        /** What the screens show: the measure as TheMealDB wrote it, or NOT_GIVEN. */
        public String label() {
            return text.isBlank() ? NOT_GIVEN : text;
        }
    }

    private MeasureParser() { }

    public static Measure parse(String measure) {
        String text = measure == null ? "" : measure.trim();
        Matcher m = AMOUNT.matcher(text);
        if (!m.matches()) {
            return new Measure(0, text.toLowerCase(Locale.ROOT), text);
        }
        return new Measure(number(m.group(1)), m.group(2).trim().toLowerCase(Locale.ROOT), text);
    }

    /** "1 1/2" -> 1.5, "3/4" -> 0.75, "2,5" -> 2.5 */
    private static double number(String text) {
        String clean = text.trim().replace(',', '.');
        if (clean.contains(" ")) {
            String[] parts = clean.split("\\s+");
            return Double.parseDouble(parts[0]) + number(parts[1]);
        }
        if (clean.contains("/")) {
            String[] parts = clean.split("/");
            double bottom = Double.parseDouble(parts[1]);
            return bottom == 0 ? 0 : Double.parseDouble(parts[0]) / bottom;
        }
        return Double.parseDouble(clean);
    }
}
