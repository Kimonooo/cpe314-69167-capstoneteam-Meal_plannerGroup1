package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class MealSlotAndLevelTest {

    @Test
    void slotIsReadIgnoringCaseAndSpaces() {
        assertEquals(MealSlot.BREAKFAST, MealSlot.fromText("Breakfast"));
        assertEquals(MealSlot.LUNCH, MealSlot.fromText("LUNCH"));
        assertEquals(MealSlot.DINNER, MealSlot.fromText("  dinner "));
        assertEquals(MealSlot.SNACK, MealSlot.fromText("snack"));
    }

    @Test
    void unknownSlotIsRejectedButHasAFallback() {
        assertThrows(IllegalArgumentException.class, () -> MealSlot.fromText("Brunch"));
        assertThrows(IllegalArgumentException.class, () -> MealSlot.fromText(null));
        assertEquals(MealSlot.LUNCH, MealSlot.fromTextOr("Brunch", MealSlot.LUNCH));
    }

    @Test
    void defaultSlotComesFromTheRecipeOrMealType() {
        assertEquals(MealSlot.BREAKFAST, MealSlot.defaultFor("Breakfast"));
        assertEquals(MealSlot.BREAKFAST, MealSlot.defaultFor("BREAKFAST"));
        assertEquals(MealSlot.SNACK, MealSlot.defaultFor("Dessert"));
        assertEquals(MealSlot.DINNER, MealSlot.defaultFor("MainCourse"));
        assertEquals(MealSlot.DINNER, MealSlot.defaultFor(null));
    }

    @Test
    void theFourBudgetsAddUpToATwoThousandCalorieDay() {
        assertEquals(2000, Arrays.stream(MealSlot.values()).mapToInt(MealSlot::getCalorieBudget).sum());
    }

    @Test
    void healthyLevelIsReadIgnoringCase() {
        assertEquals(HealthyLevel.HEALTHY, HealthyLevel.fromText("healthy"));
        assertEquals(HealthyLevel.INDULGENT, HealthyLevel.fromText("INDULGENT"));
        assertThrows(IllegalArgumentException.class, () -> HealthyLevel.fromText("Okay"));
        assertEquals(HealthyLevel.MODERATE, HealthyLevel.fromTextOr("", HealthyLevel.MODERATE));
    }
}
