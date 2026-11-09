package com.platenplatter.util;

/** Formats the average prep time for the Analytics number card, for example "31.5 min". */
public final class StatFormat {
    private StatFormat() { }

    public static String average(double minutes) {
        if (Double.isNaN(minutes) || minutes <= 0) {
            return "-";
        }
        if (minutes >= 60) {
            int whole = (int) Math.round(minutes);
            int h = whole / 60;
            int m = whole % 60;
            return m == 0 ? h + " h" : h + " h " + m + " min";
        }
        return String.format("%.1f min", minutes);
    }
}
