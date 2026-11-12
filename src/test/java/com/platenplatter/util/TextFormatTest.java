package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextFormatTest {

    @Test
    void minutesBecomeHoursAndMinutes() {
        assertEquals("45 min", TextFormat.minutes(45));
        assertEquals("1 h", TextFormat.minutes(60));
        assertEquals("1 h 25 min", TextFormat.minutes(85));
    }

    @Test
    void zeroAndNegativeMinutesShowZero() {
        assertEquals("0 min", TextFormat.minutes(0));
        assertEquals("0 min", TextFormat.minutes(-5));
    }

    @Test
    void averagesShowOneDecimalOrADashWithoutData() {
        assertEquals("37.5 min", TextFormat.averageMinutes(37.5));
        assertEquals("1 h 5 min", TextFormat.averageMinutes(65.2));
        assertEquals("-", TextFormat.averageMinutes(0));
        assertEquals("-", TextFormat.averageMinutes(Double.NaN));
    }

    @Test
    void caloriesAndPercentsHaveTheirOwnText() {
        assertEquals("1,250 kcal", TextFormat.calories(1250));
        assertEquals("? kcal", TextFormat.calories(0));
        assertEquals("58%", TextFormat.percent(14 / 24.0));
        assertEquals("-", TextFormat.percent(Double.NaN));
    }

    @Test
    void datesAndTimesAreWrittenInFull() {
        LocalDate day = LocalDate.of(2026, 11, 7);
        assertEquals("Saturday, 7 November 2026", TextFormat.date(day));
        assertEquals("Sat, 7 Nov 2026", TextFormat.shortDate(day));
        assertEquals("Mon 2", TextFormat.dayLabel(LocalDate.of(2026, 11, 2)));
        assertEquals("1:30 PM", TextFormat.time(LocalTime.of(13, 30)));
    }

    @Test
    void theGreetingChangesDuringTheDay() {
        assertEquals("Good morning", TextFormat.greeting(LocalTime.of(5, 0)));
        assertEquals("Good morning", TextFormat.greeting(LocalTime.of(11, 59)));
        assertEquals("Good afternoon", TextFormat.greeting(LocalTime.of(12, 0)));
        assertEquals("Good evening", TextFormat.greeting(LocalTime.of(18, 0)));
        assertEquals("Good evening", TextFormat.greeting(LocalTime.of(4, 59)));
    }

    @Test
    void eachLineOfTheInstructionsIsOneStepAndItsOwnLabelIsRemoved() {
        assertEquals(List.of("Boil the pasta.", "Fry the garlic.", "2 tablespoons of oil go in last."),
                TextFormat.steps("STEP 1\r\nBoil the pasta.\r\n\r\nStep 2: Fry the garlic.\r\n3. 2 tablespoons of oil go in last."));
    }

    @Test
    void oneLongParagraphIsCutAfterEverySentenceAndNoTextGivesNoSteps() {
        assertEquals(List.of("Heat the oil.", "Add 1.5 cups of rice!", "Serve hot."),
                TextFormat.steps("Heat the oil. Add 1.5 cups of rice! Serve hot."));
        assertEquals(List.of(), TextFormat.steps("   "));
        assertEquals(List.of(), TextFormat.steps(null));
    }
}
