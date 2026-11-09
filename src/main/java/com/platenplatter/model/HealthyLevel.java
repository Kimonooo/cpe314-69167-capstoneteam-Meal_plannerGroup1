package com.platenplatter.model;

/**
 * How healthy a planned meal is. The calendar, the meal chips and the Analytics chart all use the
 * same three colors: Healthy = green, Moderate = yellow, Indulgent = red.
 * The database stores the enum name (HEALTHY, MODERATE, INDULGENT).
 */
public enum HealthyLevel {
    HEALTHY("Healthy"),
    MODERATE("Moderate"),
    INDULGENT("Indulgent");

    private final String label;

    HealthyLevel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Reads "Healthy", "HEALTHY" or "healthy".
     * @throws IllegalArgumentException for any other word
     */
    public static HealthyLevel fromText(String text) {
        if (text != null) {
            String t = text.trim();
            for (HealthyLevel level : values()) {
                if (level.name().equalsIgnoreCase(t) || level.label.equalsIgnoreCase(t)) {
                    return level;
                }
            }
        }
        throw new IllegalArgumentException("Unknown healthy level (use Healthy, Moderate or Indulgent): " + text);
    }

    /** Like fromText, but returns the fallback instead of throwing (used when reading old database rows). */
    public static HealthyLevel fromTextOr(String text, HealthyLevel fallback) {
        try {
            return fromText(text);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Override
    public String toString() {
        return label;
    }
}
