package com.platenplatter.model;

import java.util.Arrays;

/** How healthy a planned meal is. The database stores name(), for example HEALTHY. */
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

    /** Reads "HEALTHY", "Healthy" or "healthy". Anything else throws, with a reason the import can show. */
    public static HealthyLevel fromText(String text) {
        String clean = text == null ? "" : text.trim();
        return Arrays.stream(values())
                .filter(level -> level.name().equalsIgnoreCase(clean) || level.label.equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown healthy level (use Healthy, Moderate or Indulgent): " + text));
    }

    /** Like fromText, but gives the fallback instead of throwing. */
    public static HealthyLevel fromTextOr(String text, HealthyLevel fallback) {
        String clean = text == null ? "" : text.trim();
        return Arrays.stream(values())
                .filter(level -> level.name().equalsIgnoreCase(clean) || level.label.equalsIgnoreCase(clean))
                .findFirst()
                .orElse(fallback);
    }

    @Override
    public String toString() {
        return label;
    }
}
