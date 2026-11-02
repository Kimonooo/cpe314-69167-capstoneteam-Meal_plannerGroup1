package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class CsvRecipeParserTest {

    @Test
    void parsesValidLine() {
        ImportRow row = CsvRecipeParser.parseLine("2026-11-03,Chicken Adobo,MainCourse,45");
        assertEquals("2026-11-03", row.getDate());
        assertEquals("Chicken Adobo", row.getRecipeName());
        assertEquals("MainCourse", row.getCategory());
        assertEquals(45, row.getPrepMinutes());
    }

    @Test
    void keepsCommaInsideQuotes() {
        ImportRow row = CsvRecipeParser.parseLine("2026-11-03,\"Rice, Egg and Longganisa\",Breakfast,20");
        assertEquals("Rice, Egg and Longganisa", row.getRecipeName());
    }

    @Test
    void wrongFieldCountIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Only Two"));
    }

    @Test
    void nonNumericOrNegativePrepIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Halo-Halo,Dessert,abc"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Halo-Halo,Dessert,-5"));
    }

    @Test
    void emptyLineAndBadDateAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parseLine("   "));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("11/03/2026,Halo-Halo,Dessert,10"));
    }
}
