package com.platenplatter.util;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/** Turns dates and times into the friendly text shown in the header. */
public final class DateTimeText {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("EEE, d MMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm:ss a", Locale.ENGLISH);

    private DateTimeText() { }

    /** For example "Tuesday, 3 November 2026". */
    public static String date(LocalDate date) {
        return date.format(DATE);
    }

    /** For example "Tue, 3 Nov 2026". */
    public static String shortDate(LocalDate date) {
        return date.format(SHORT);
    }

    /** For example "10:42:15 AM". */
    public static String time(LocalTime time) {
        return time.format(TIME);
    }

    /** Good morning (05:00-11:59), Good afternoon (12:00-17:59), otherwise Good evening. */
    public static String greeting(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 12) {
            return "Good morning";
        }
        if (hour >= 12 && hour < 18) {
            return "Good afternoon";
        }
        return "Good evening";
    }
}
