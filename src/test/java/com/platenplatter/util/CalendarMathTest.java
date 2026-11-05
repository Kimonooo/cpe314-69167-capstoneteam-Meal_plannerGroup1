package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.YearMonth;
import org.junit.jupiter.api.Test;

class CalendarMathTest {

    @Test
    void novemberTwentyTwentySixStartsOnASunday() {
        // 1 November 2026 is a Sunday, so six blanks come first (Mon..Sat)
        assertEquals(6, CalendarMath.leadingBlanks(YearMonth.of(2026, 11)));
        assertEquals(6, CalendarMath.column(LocalDate.of(2026, 11, 1)));
    }

    @Test
    void aMonthStartingOnMondayHasNoBlanks() {
        assertEquals(0, CalendarMath.leadingBlanks(YearMonth.of(2026, 6)));
    }

    @Test
    void rowsNeededRangeFromFourToSix() {
        assertEquals(4, CalendarMath.weekRows(YearMonth.of(2027, 2)));   // Feb 2027: 28 days, starts Monday
        assertEquals(5, CalendarMath.weekRows(YearMonth.of(2026, 6)));
        assertEquals(6, CalendarMath.weekRows(YearMonth.of(2026, 11)));  // Sunday start, 30 days
    }

    @Test
    void rowOfADateInsideTheGrid() {
        assertEquals(0, CalendarMath.row(LocalDate.of(2026, 11, 1)));
        assertEquals(1, CalendarMath.row(LocalDate.of(2026, 11, 2)));
        assertEquals(5, CalendarMath.row(LocalDate.of(2026, 11, 30)));
    }
}
