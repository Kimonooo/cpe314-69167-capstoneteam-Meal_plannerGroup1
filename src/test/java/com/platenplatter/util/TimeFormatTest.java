package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TimeFormatTest {

    @Test
    void shortTimesStayInMinutes() {
        assertEquals("45 min", TimeFormat.minutes(45));
        assertEquals("1 min", TimeFormat.minutes(1));
    }

    @Test
    void wholeHoursHaveNoMinutesPart() {
        assertEquals("1 h", TimeFormat.minutes(60));
        assertEquals("2 h", TimeFormat.minutes(120));
    }

    @Test
    void hoursAndMinutesAreJoined() {
        assertEquals("1 h 15 min", TimeFormat.minutes(75));
        assertEquals("2 h 5 min", TimeFormat.minutes(125));
    }

    @Test
    void zeroAndNegativeNumbersBecomeZeroMinutes() {
        assertEquals("0 min", TimeFormat.minutes(0));
        assertEquals("0 min", TimeFormat.minutes(-30));
    }
}
