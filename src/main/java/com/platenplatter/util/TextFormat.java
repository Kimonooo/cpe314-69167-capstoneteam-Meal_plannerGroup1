package com.platenplatter.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Turns numbers, minutes, dates and times into the text the screens show. */
public final class TextFormat {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("EEE d", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH);

    private TextFormat() { }

    /** 45 -> "45 min", 60 -> "1 h", 85 -> "1 h 25 min"; 0 or less -> "0 min". */
    public static String minutes(int totalMinutes) {
        if (totalMinutes <= 0) {
            return "0 min";
        }
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        if (hours == 0) {
            return minutes + " min";
        }
        return minutes == 0 ? hours + " h" : hours + " h " + minutes + " min";
    }

    /** An average number of minutes: "37.5 min" or "1 h 5 min"; no data -> "-". */
    public static String averageMinutes(double minutes) {
        if (Double.isNaN(minutes) || minutes <= 0) {
            return "-";
        }
        return minutes >= 60 ? minutes((int) Math.round(minutes)) : String.format(Locale.ENGLISH, "%.1f min", minutes);
    }

    /** 520 -> "520 kcal"; 0 (not known) -> "? kcal". */
    public static String calories(int calories) {
        return calories > 0 ? String.format(Locale.ENGLISH, "%,d kcal", calories) : "? kcal";
    }

    /** A share from 0 to 1 as a percent: 0.583 -> "58%"; no data -> "-". */
    public static String percent(double share) {
        return Double.isNaN(share) ? "-" : String.format(Locale.ENGLISH, "%.0f%%", share * 100);
    }

    /** "Saturday, 7 November 2026" */
    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    /** "Sat, 7 Nov 2026" */
    public static String shortDate(LocalDate date) {
        return date.format(SHORT_DATE);
    }

    /** "Mon 2", the label of a day on the chart. */
    public static String dayLabel(LocalDate date) {
        return date.format(DAY);
    }

    /** "1:30 PM" */
    public static String time(LocalTime time) {
        return time.format(TIME);
    }

    /** Good morning (5:00 to 11:59), Good afternoon (12:00 to 17:59), Good evening (otherwise). */
    public static String greeting(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        }
        return hour >= 12 && hour < 18 ? "Good afternoon" : "Good evening";
    }
}
