package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void theHintSaysWhyAutoChoseALevel() {
        assertEquals("520 kcal vs 650 kcal budget for Lunch", HealthClassifier.explain(520, MealSlot.LUNCH, "Vegetarian"));
        assertTrue(HealthClassifier.explain(0, MealSlot.LUNCH, "Vegetarian").contains("recipe type"));
    }
}
