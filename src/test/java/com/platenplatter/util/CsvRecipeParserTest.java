package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvRecipeParserTest {

    @Test
    void aFullLineBecomesAPlannedMeal() {
        PlannedMeal meal = CsvRecipeParser.parse("2026-11-03,Chicken Adobo,MainCourse,45,Dinner,620,Healthy");
        assertEquals(LocalDate.of(2026, 11, 3), meal.getPlanDate());
        assertEquals("Chicken Adobo", meal.getRecipeName());
        assertEquals("MAIN_COURSE", meal.getRecipe().getMealType());
        assertEquals(45, meal.getPrepMinutes());
        assertEquals(MealSlot.DINNER, meal.getMealSlot());
        assertEquals(620, meal.getCalories());
        assertEquals(HealthyLevel.HEALTHY, meal.getHealthyLevel());
    }

    @Test
    void anOldFourColumnLineGetsTheSlotOfItsTypeAndAnAutoLevel() {
        PlannedMeal dessert = CsvRecipeParser.parse("2026-11-04,Leche Flan,Dessert,50");
        assertInstanceOf(DessertRecipe.class, dessert.getRecipe());
        assertEquals(MealSlot.SNACK, dessert.getMealSlot());
        assertEquals(HealthyLevel.INDULGENT, dessert.getHealthyLevel()); // no calories: dessert rule
        assertEquals(0, dessert.getCalories());
    }

    @Test
    void anEmptyLevelIsWorkedOutFromTheCalories() {
        assertEquals(HealthyLevel.MODERATE, CsvRecipeParser.parse("2026-11-02,Tapsilog,Breakfast,20,Breakfast,620,").getHealthyLevel());
    }

    @Test
    void aCommaInsideQuotesIsKept() {
        PlannedMeal meal = CsvRecipeParser.parse("2026-05-30,\"Rice, Egg and Longganisa\",Breakfast,20");
        assertEquals("Rice, Egg and Longganisa", meal.getRecipeName());
        assertEquals(List.of("a", "b, c", "say \"hi\""), CsvRecipeParser.splitCsv("a,\"b, c\",\"say \"\"hi\"\"\""));
    }

    @Test
    void aWrongNumberOfFieldsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-05,Only Three Fields,Dessert"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-05,A,Dessert,1,Snack,1,Healthy,extra"));
    }

    @Test
    void aBadDateIsRejectedEvenIfItLooksRight() {
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("not-a-date,Chicken Adobo,MainCourse,45"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-02-30,Chicken Adobo,MainCourse,45"));
    }

    @Test
    void badOrNegativeNumbersAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-04,Leche Flan,Dessert,abc"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-06,Pancit Canton,MainCourse,-30"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-06,Pancit Canton,MainCourse,30,Dinner,-5,"));
    }

    @Test
    void anUnknownSlotOrLevelOrAnEmptyNameIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-06,Pancit,MainCourse,30,Brunch,,"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-06,Pancit,MainCourse,30,Dinner,500,Okay"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("2026-11-06, ,MainCourse,30"));
        assertThrows(IllegalArgumentException.class, () -> CsvRecipeParser.parse("   "));
    }

    @Test
    void anEmptyCategoryBecomesAMainCourse() {
        assertEquals("MAIN_COURSE", CsvRecipeParser.parse("2026-11-06,Mystery Dish,,30").getRecipe().getMealType());
    }
}
