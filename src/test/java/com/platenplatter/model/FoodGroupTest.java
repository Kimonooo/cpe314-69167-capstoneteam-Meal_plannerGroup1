package com.platenplatter.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import org.junit.jupiter.api.Test;

class FoodGroupTest {

    @Test
    void commonIngredientsGoToTheirGroup() {
        assertEquals(FoodGroup.FRUITS_VEGETABLES, FoodGroup.of("Garlic"));
        assertEquals(FoodGroup.MEAT_SEAFOOD, FoodGroup.of("chicken thighs"));
        assertEquals(FoodGroup.DAIRY_EGGS, FoodGroup.of("Eggs"));
        assertEquals(FoodGroup.GRAINS_PANTRY, FoodGroup.of("penne rigate"));
        assertEquals(FoodGroup.SAUCES_CONDIMENTS, FoodGroup.of("Soy Sauce"));
        assertEquals(FoodGroup.BREAD_BAKERY, FoodGroup.of("Pandesal"));
    }

    @Test
    void pepperIsASpiceButBellPepperIsAVegetable() {
        assertEquals(FoodGroup.HERBS_SPICES, FoodGroup.of("Pepper"));
        assertEquals(FoodGroup.HERBS_SPICES, FoodGroup.of("black pepper"));
        assertEquals(FoodGroup.FRUITS_VEGETABLES, FoodGroup.of("bell pepper"));
        assertEquals(FoodGroup.FRUITS_VEGETABLES, FoodGroup.of("Red Peppers"));
    }

    @Test
    void theRuleOrderMatters() {
        assertEquals(FoodGroup.GRAINS_PANTRY, FoodGroup.of("coconut milk"));     // not Dairy
        assertEquals(FoodGroup.GRAINS_PANTRY, FoodGroup.of("peanut butter"));    // not Dairy
        assertEquals(FoodGroup.FRUITS_VEGETABLES, FoodGroup.of("eggplant"));     // not "egg"
        assertEquals(FoodGroup.FRUITS_VEGETABLES, FoodGroup.of("Garlic Clove")); // not a spice
    }

    @Test
    void anUnknownOrEmptyNameIsOther() {
        assertEquals(FoodGroup.OTHER, FoodGroup.of("xyz"));
        assertEquals(FoodGroup.OTHER, FoodGroup.of(""));
        assertEquals(FoodGroup.OTHER, FoodGroup.of(null));
    }

    @Test
    void theSavedNameIsReadBackAndBadValuesBecomeOther() {
        Arrays.stream(FoodGroup.values()).forEach(group -> assertEquals(group, FoodGroup.fromName(group.name())));
        assertEquals(FoodGroup.OTHER, FoodGroup.fromName("AISLE_7"));
        assertEquals(FoodGroup.OTHER, FoodGroup.fromName(null));
        assertEquals("Herbs & Spices", FoodGroup.HERBS_SPICES.toString());
    }
}
