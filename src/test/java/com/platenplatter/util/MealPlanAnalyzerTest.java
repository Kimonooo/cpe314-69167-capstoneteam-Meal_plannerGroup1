package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;

class MealPlanAnalyzerTest {

    @Test
    void countsEachCategory() {
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(
                Arrays.asList("Dessert", "MainCourse", "Dessert", "Breakfast", "Dessert"));
        assertEquals(3, counts.get("Dessert"));
        assertEquals(1, counts.get("MainCourse"));
        assertEquals(1, counts.get("Breakfast"));
    }

    @Test
    void emptyListGivesEmptyMapAndNullMostFrequent() {
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(Collections.emptyList());
        assertTrue(counts.isEmpty());
        assertNull(MealPlanAnalyzer.mostFrequent(counts));
    }

    @Test
    void blankAndNullCategoriesBecomeUncategorized() {
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(Arrays.asList("", null, "  "));
        assertEquals(3, counts.get("Uncategorized"));
    }

    @Test
    void changingInputChangesMostFrequent() {
        Map<String, Integer> a = MealPlanAnalyzer.countByCategory(Arrays.asList("Dessert", "Dessert", "Breakfast"));
        Map<String, Integer> b = MealPlanAnalyzer.countByCategory(Arrays.asList("Dessert", "Breakfast", "Breakfast"));
        assertEquals("Dessert", MealPlanAnalyzer.mostFrequent(a));
        assertEquals("Breakfast", MealPlanAnalyzer.mostFrequent(b));
    }

    @Test
    void averageIgnoresNegativesAndNulls() {
        assertEquals(20.0, MealPlanAnalyzer.averagePrepMinutes(Arrays.asList(10, 30, -5, null)), 0.0001);
        assertEquals(0.0, MealPlanAnalyzer.averagePrepMinutes(Collections.emptyList()), 0.0001);
    }
}
