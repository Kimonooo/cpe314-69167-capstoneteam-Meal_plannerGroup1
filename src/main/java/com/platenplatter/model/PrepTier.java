package com.platenplatter.model;

/**
 * How long a meal takes to make, in three tiers:
 *   Quick   = 15 minutes or less
 *   Average = 16 to 45 minutes
 *   Slow    = more than 45 minutes
 * The tier is never stored in the database. It is always worked out from prep_minutes,
 * so it can never go out of date when the prep time is changed.
 */
public enum PrepTier {
    QUICK("Quick", "15 min or less"),
    AVERAGE("Average", "16 to 45 min"),
    SLOW("Slow", "more than 45 min");

    /** The last minute that still counts as Quick. */
    public static final int QUICK_MAX = 15;
    /** The last minute that still counts as Average. */
    public static final int AVERAGE_MAX = 45;

    private final String label;
    private final String range;

    PrepTier(String label, String range) {
        this.label = label;
        this.range = range;
    }

    public String getLabel() {
        return label;
    }

    /** For example "16 to 45 min" (shown in tooltips and on the Analytics tab). */
    public String getRange() {
        return range;
    }

    /**
     * The tier of a prep time.
     * @throws IllegalArgumentException if the minutes are negative
     */
    public static PrepTier of(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException("Prep minutes cannot be negative: " + minutes);
        }
        if (minutes <= QUICK_MAX) {
            return QUICK;
        }
        if (minutes <= AVERAGE_MAX) {
            return AVERAGE;
        }
        return SLOW;
    }

    @Override
    public String toString() {
        return label;
    }
}
