package com.platenplatter.util;

/** Turns a number of minutes into friendly text, for example 75 becomes "1 h 15 min". */
public final class TimeFormat {
    private TimeFormat() { }

    public static String minutes(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0 min";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + " min";
        }
        if (minutes == 0) {
            return hours + " h";
        }
        return hours + " h " + minutes + " min";
    }
}
