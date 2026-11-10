package com.platenplatter.util;

import java.time.LocalDate;
import java.time.YearMonth;

/** Small helpers to lay out a month calendar whose first column is Monday. */
public final class CalendarMath {
    private CalendarMath() { }

    /** How many empty cells come before day 1 (Monday = 0 ... Sunday = 6). */
    public static int leadingBlanks(YearMonth month) {
        return month.atDay(1).getDayOfWeek().getValue() - 1;
    }

    /** How many rows (weeks) the month needs: 4, 5 or 6. */
    public static int weekRows(YearMonth month) {
        return (leadingBlanks(month) + month.lengthOfMonth() + 6) / 7;
    }

    /** The column (0 = Monday) of a date. */
    public static int column(LocalDate date) {
        return date.getDayOfWeek().getValue() - 1;
    }

    /** The row of a date inside its month's grid (0 = first week). */
    public static int row(LocalDate date) {
        return (leadingBlanks(YearMonth.from(date)) + date.getDayOfMonth() - 1) / 7;
    }
}
