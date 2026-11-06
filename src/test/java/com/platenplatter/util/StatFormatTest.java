package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class StatFormatTest {

    @Test
    void under60MinutesShowsOneDecimal() {
        assertEquals("31.5 min", StatFormat.average(31.5));
        assertEquals("20.0 min", StatFormat.average(20));
    }

    @Test
    void anHourOrMoreShowsHoursAndMinutes() {
        assertEquals("1 h 15 min", StatFormat.average(75));
        assertEquals("2 h", StatFormat.average(120));
    }

    @Test
    void noDataOrBadNumbersShowADash() {
        assertEquals("-", StatFormat.average(0));
        assertEquals("-", StatFormat.average(-4));
        assertEquals("-", StatFormat.average(Double.NaN));
    }
}
