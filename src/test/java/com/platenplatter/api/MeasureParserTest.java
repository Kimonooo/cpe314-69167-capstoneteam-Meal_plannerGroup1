package com.platenplatter.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class MeasureParserTest {

    @Test
    void aNumberStuckToItsUnitIsRead() {
        MeasureParser.Measure grams = MeasureParser.parse("200g");
        assertEquals(200, grams.amount(), 0.001);
        assertEquals("g", grams.unit());
    }

    @Test
    void fractionsAndMixedNumbersAreRead() {
        assertEquals(0.5, MeasureParser.parse("1/2 tsp").amount(), 0.001);
        assertEquals(1.5, MeasureParser.parse("1 1/2 cups").amount(), 0.001);
        assertEquals("cups", MeasureParser.parse("1 1/2 cups").unit());
        assertEquals(2.5, MeasureParser.parse("2,5 kg").amount(), 0.001);
    }

    @Test
    void wordsWithoutANumberHaveNoAmount() {
        MeasureParser.Measure pinch = MeasureParser.parse("pinch");
        assertFalse(pinch.hasAmount());
        assertEquals("pinch", pinch.label());
    }

    @Test
    void aBlankMeasureSaysThatTheAmountIsNotGiven() {
        assertEquals(MeasureParser.NOT_GIVEN, MeasureParser.parse(" ").label());
        assertEquals(MeasureParser.NOT_GIVEN, MeasureParser.parse(null).label());
    }

    @Test
    void aZeroBottomDoesNotCrash() {
        assertEquals(0, MeasureParser.parse("1/0 cup").amount(), 0.001);
    }
}
