package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DateTimeTextTest {

    @Test
    void dateIsWrittenInFull() {
        assertEquals("Tuesday, 3 November 2026", DateTimeText.date(LocalDate.of(2026, 11, 3)));
        assertEquals("Tue, 3 Nov 2026", DateTimeText.shortDate(LocalDate.of(2026, 11, 3)));
    }

    @Test
    void timeUsesTwelveHourClock() {
        assertEquals("10:42:15 AM", DateTimeText.time(LocalTime.of(10, 42, 15)));
        assertEquals("12:00:00 AM", DateTimeText.time(LocalTime.MIDNIGHT));
        assertEquals("4:05:09 PM", DateTimeText.time(LocalTime.of(16, 5, 9)));
    }

    @Test
    void greetingChangesDuringTheDay() {
        assertEquals("Good morning", DateTimeText.greeting(LocalTime.of(5, 0)));
        assertEquals("Good morning", DateTimeText.greeting(LocalTime.of(11, 59)));
        assertEquals("Good afternoon", DateTimeText.greeting(LocalTime.NOON));
        assertEquals("Good evening", DateTimeText.greeting(LocalTime.of(18, 0)));
        assertEquals("Good evening", DateTimeText.greeting(LocalTime.of(3, 0)));
    }
}
