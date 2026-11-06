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

    @Test
    void readsTheThreeOptionalColumns() {
        ImportRow row = CsvRecipeParser.parseLine("2026-11-03,Chicken Adobo,MainCourse,45,Dinner,620,Healthy");
        assertEquals(com.platenplatter.model.MealSlot.DINNER, row.getMealSlot());
        assertEquals(620, row.getCalories());
        assertEquals(com.platenplatter.model.HealthyLevel.HEALTHY, row.getHealthyLevel());
    }

    @Test
    void anOldFourColumnLineGetsTheDefaults() {
        ImportRow row = CsvRecipeParser.parseLine("2026-11-02,Tapsilog,Breakfast,20");
        assertEquals(com.platenplatter.model.MealSlot.BREAKFAST, row.getMealSlot());
        assertEquals(0, row.getCalories());
        assertEquals(com.platenplatter.model.HealthyLevel.MODERATE, row.getHealthyLevel());
    }

    @Test
    void emptyOptionalColumnsAreWorkedOut() {
        ImportRow row = CsvRecipeParser.parseLine("2026-11-03,Sinigang,MainCourse,40,Lunch,500,");
        assertEquals(com.platenplatter.model.MealSlot.LUNCH, row.getMealSlot());
        assertEquals(com.platenplatter.model.HealthyLevel.HEALTHY, row.getHealthyLevel()); // 500 <= 650 lunch budget
        ImportRow noSlot = CsvRecipeParser.parseLine("2026-11-03,Leche Flan,Dessert,50,,,");
        assertEquals(com.platenplatter.model.MealSlot.SNACK, noSlot.getMealSlot());
    }

    @Test
    void aBadSlotCaloriesOrLevelIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Adobo,MainCourse,45,Brunch"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Adobo,MainCourse,45,Dinner,lots"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Adobo,MainCourse,45,Dinner,-5"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Adobo,MainCourse,45,Dinner,600,Okay"));
        assertThrows(IllegalArgumentException.class,
                () -> CsvRecipeParser.parseLine("2026-11-03,Adobo,MainCourse,45,Dinner,600,Healthy,extra"));
    }
}
