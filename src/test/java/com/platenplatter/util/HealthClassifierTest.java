package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import org.junit.jupiter.api.Test;

class HealthClassifierTest {

    @Test
    void aLunchAtItsBudgetIsHealthyAndOneMoreCalorieIsModerate() {
        assertEquals(HealthyLevel.HEALTHY, HealthClassifier.classify(650, MealSlot.LUNCH, "MainCourse"));
        assertEquals(HealthyLevel.MODERATE, HealthClassifier.classify(651, MealSlot.LUNCH, "MainCourse"));
    }

    @Test
    void moreThanOneAndAHalfBudgetsIsIndulgent() {
        assertEquals(HealthyLevel.MODERATE, HealthClassifier.classify(975, MealSlot.DINNER, "MainCourse"));
        assertEquals(HealthyLevel.INDULGENT, HealthClassifier.classify(976, MealSlot.DINNER, "MainCourse"));
    }

    @Test
    void theSameCaloriesCanBeHealthyForLunchButIndulgentForASnack() {
        assertEquals(HealthyLevel.HEALTHY, HealthClassifier.classify(400, MealSlot.LUNCH, "MainCourse"));
        assertEquals(HealthyLevel.INDULGENT, HealthClassifier.classify(400, MealSlot.SNACK, "MainCourse"));
    }

    @Test
    void knownCaloriesBeatTheRecipeType() {
        // a light dessert is Healthy when it fits the snack budget
        assertEquals(HealthyLevel.HEALTHY, HealthClassifier.classify(150, MealSlot.SNACK, "Dessert"));
    }

    @Test
    void unknownCaloriesFollowTheRecipeType() {
        assertEquals(HealthyLevel.INDULGENT, HealthClassifier.classify(0, MealSlot.SNACK, "Dessert"));
        assertEquals(HealthyLevel.HEALTHY, HealthClassifier.classify(0, MealSlot.LUNCH, "Vegetarian"));
        assertEquals(HealthyLevel.HEALTHY, HealthClassifier.classify(0, MealSlot.DINNER, "seafood"));
        assertEquals(HealthyLevel.MODERATE, HealthClassifier.classify(0, MealSlot.DINNER, "Chicken"));
        assertEquals(HealthyLevel.MODERATE, HealthClassifier.classify(0, null, null));
    }

    @Test
    void negativeCaloriesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> HealthClassifier.classify(-1, MealSlot.LUNCH, "MainCourse"));
    }

    @Test
    void anOldRowGetsItsLevelAndSlotAutomatically() {
        ImportRow dessert = new ImportRow("2026-11-04", "Leche Flan", "Dessert", 50);
        assertEquals(MealSlot.SNACK, dessert.getMealSlot());
        assertEquals(HealthyLevel.INDULGENT, dessert.getHealthyLevel());
        assertEquals(0, dessert.getCalories());
    }
}
