package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RecipeTest {

    @Test
    void eachSubclassUsesItsOwnPrepTimeRule() {
        Recipe breakfast = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        Recipe mainCourse = new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550);
        Recipe dessert = new DessertRecipe(3, "Chocolate Cake", "American", 30, 400);

        assertEquals(10, breakfast.getPrepMinutes()); // base time only
        assertEquals(30, mainCourse.getPrepMinutes()); // base + 10
        assertEquals(45, dessert.getPrepMinutes()); // base + 15
    }

    @Test
    void blankNameIsRejected() {
        // edge case: a recipe must have a real name
        assertThrows(IllegalArgumentException.class,
                () -> new BreakfastRecipe(1, "   ", "American", 10, 300));
    }

    @Test
    void negativeCaloriesAreRejected() {
        // edge case: calories can never be below zero
        assertThrows(IllegalArgumentException.class,
                () -> new DessertRecipe(1, "Cake", "American", 10, -5));
    }
}
