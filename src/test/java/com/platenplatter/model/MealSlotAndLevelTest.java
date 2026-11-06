package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.*;

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
    void defaultSlotComesFromTheRecipeType() {
        assertEquals(MealSlot.BREAKFAST, MealSlot.defaultFor("Breakfast"));
        assertEquals(MealSlot.SNACK, MealSlot.defaultFor("Dessert"));
        assertEquals(MealSlot.DINNER, MealSlot.defaultFor("MainCourse"));
        assertEquals(MealSlot.DINNER, MealSlot.defaultFor(null));
    }

    @Test
    void theFourBudgetsAddUpToATwoThousandCalorieDay() {
        int sum = 0;
        for (MealSlot slot : MealSlot.values()) {
            sum += slot.getCalorieBudget();
        }
        assertEquals(2000, sum);
    }

    @Test
    void healthyLevelIsReadIgnoringCase() {
        assertEquals(HealthyLevel.HEALTHY, HealthyLevel.fromText("healthy"));
        assertEquals(HealthyLevel.INDULGENT, HealthyLevel.fromText("INDULGENT"));
        assertThrows(IllegalArgumentException.class, () -> HealthyLevel.fromText("Okay"));
        assertEquals(HealthyLevel.MODERATE, HealthyLevel.fromTextOr("", HealthyLevel.MODERATE));
    }
}
