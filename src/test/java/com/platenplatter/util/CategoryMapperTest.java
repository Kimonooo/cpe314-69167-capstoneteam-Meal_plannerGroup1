package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CategoryMapperTest {

    @Test
    void breakfastAndDessertGetTheirOwnMealType() {
        assertEquals("BREAKFAST", CategoryMapper.toMealType("Breakfast"));
        assertEquals("DESSERT", CategoryMapper.toMealType("Dessert"));
    }

    @Test
    void matchingIgnoresCaseAndSpaces() {
        assertEquals("DESSERT", CategoryMapper.toMealType("  dEsSeRt "));
    }

    @Test
    void everythingElseIsMainCourse() {
        assertEquals("MAIN_COURSE", CategoryMapper.toMealType("Chicken"));
        assertEquals("MAIN_COURSE", CategoryMapper.toMealType("Vegetarian"));
        assertEquals("MAIN_COURSE", CategoryMapper.toMealType("MainCourse"));
    }

    @Test
    void nullOrBlankFallsBackToMainCourse() {
        assertEquals("MAIN_COURSE", CategoryMapper.toMealType(null));
        assertEquals("MAIN_COURSE", CategoryMapper.toMealType(""));
    }
}
