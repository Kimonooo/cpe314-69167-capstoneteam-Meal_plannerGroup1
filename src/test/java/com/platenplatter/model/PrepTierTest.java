package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class PrepTierTest {

    @Test
    void fifteenMinutesOrLessIsQuick() {
        assertEquals(PrepTier.QUICK, PrepTier.of(0));
        assertEquals(PrepTier.QUICK, PrepTier.of(15));
    }

    @Test
    void sixteenToFortyFiveIsAverage() {
        assertEquals(PrepTier.AVERAGE, PrepTier.of(16));
        assertEquals(PrepTier.AVERAGE, PrepTier.of(45));
    }

    @Test
    void moreThanFortyFiveIsSlow() {
        assertEquals(PrepTier.SLOW, PrepTier.of(46));
        assertEquals(PrepTier.SLOW, PrepTier.of(480));
    }

    @Test
    void negativeMinutesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PrepTier.of(-1));
    }
}
