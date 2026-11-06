package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CategoryMapperTest {

    @Test
    void breakfastAndDessertKeepTheirNames() {
        assertEquals("Breakfast", CategoryMapper.toPlanCategory("Breakfast"));
        assertEquals("Dessert", CategoryMapper.toPlanCategory("Dessert"));
    }

    @Test
    void matchingIgnoresCaseAndSpaces() {
        assertEquals("Dessert", CategoryMapper.toPlanCategory("  dEsSeRt "));
    }

    @Test
    void everythingElseIsMainCourse() {
        assertEquals("MainCourse", CategoryMapper.toPlanCategory("Chicken"));
        assertEquals("MainCourse", CategoryMapper.toPlanCategory("Seafood"));
    }

    @Test
    void nullOrBlankFallsBackToMainCourse() {
        assertEquals("MainCourse", CategoryMapper.toPlanCategory(null));
        assertEquals("MainCourse", CategoryMapper.toPlanCategory(""));
    }
}
