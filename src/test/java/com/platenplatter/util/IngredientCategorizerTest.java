package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IngredientCategorizerTest {

    private static Aisle aisle(String raw) {
        return IngredientCategorizer.aisleFor(IngredientCategorizer.normalize(raw));
    }

    @Test
    void normalizeCleansAndMergesSpellings() {
        assertEquals("egg", IngredientCategorizer.normalize("  Eggs "));
        assertEquals("tomato", IngredientCategorizer.normalize("Tomatoes"));
        assertEquals("soy sauce", IngredientCategorizer.normalize("Soy   Sauce,"));
        assertEquals("", IngredientCategorizer.normalize("   "));
        assertEquals("", IngredientCategorizer.normalize(null));
    }

    @Test
    void displayCapitalisesTheFirstLetter() {
        assertEquals("Chicken breast", IngredientCategorizer.display("chicken breast"));
        assertEquals("", IngredientCategorizer.display(""));
    }

    @Test
    void meatDairyAndProduce() {
        assertEquals(Aisle.MEAT, aisle("Chicken"));
        assertEquals(Aisle.MEAT, aisle("Pork belly"));
        assertEquals(Aisle.DAIRY, aisle("Butter"));
        assertEquals(Aisle.DAIRY, aisle("Egg"));
        assertEquals(Aisle.DAIRY, aisle("Cheese"));
        assertEquals(Aisle.PRODUCE, aisle("Garlic"));
        assertEquals(Aisle.PRODUCE, aisle("Tomatoes"));
        assertEquals(Aisle.PRODUCE, aisle("Bean sprouts"));
    }

    @Test
    void theFirstMatchingRuleWinsSoTrickyNamesLandInTheRightAisle() {
        assertEquals(Aisle.PANTRY, aisle("Chicken stock"));       // stock beats chicken
        assertEquals(Aisle.SAUCES, aisle("Tomato paste"));        // paste beats tomato
        assertEquals(Aisle.SAUCES, aisle("Soy sauce"));
        assertEquals(Aisle.SAUCES, aisle("Shrimp paste"));        // paste beats shrimp
        assertEquals(Aisle.SPICES, aisle("Garlic powder"));       // powder beats garlic
        assertEquals(Aisle.PANTRY, aisle("Baking powder"));       // special pantry rule beats powder
        assertEquals(Aisle.PANTRY, aisle("Peanut butter"));       // beats butter
        assertEquals(Aisle.PANTRY, aisle("Coconut milk"));        // beats milk
        assertEquals(Aisle.PANTRY, aisle("Egg noodles"));         // beats egg
        assertEquals(Aisle.PRODUCE, aisle("Eggplant"));           // is not an egg
        assertEquals(Aisle.PRODUCE, aisle("Butternut squash"));   // is not butter
    }

    @Test
    void pepperIsASpiceButBellPepperIsAVegetable() {
        assertEquals(Aisle.SPICES, aisle("Black pepper"));
        assertEquals(Aisle.SPICES, aisle("Pepper"));
        assertEquals(Aisle.SPICES, aisle("Bay leaves"));
        assertEquals(Aisle.PRODUCE, aisle("Bell pepper"));
        assertEquals(Aisle.PRODUCE, aisle("Red pepper"));
        assertEquals(Aisle.PRODUCE, aisle("Chili pepper"));
    }

    @Test
    void pantryBakeryAndUnknownThings() {
        assertEquals(Aisle.PANTRY, aisle("Rice"));
        assertEquals(Aisle.PANTRY, aisle("Sugar"));
        assertEquals(Aisle.PANTRY, aisle("Flour"));
        assertEquals(Aisle.PANTRY, aisle("Kidney beans"));
        assertEquals(Aisle.BAKERY, aisle("Pandesal"));
        assertEquals(Aisle.BAKERY, aisle("Bread"));
        assertEquals(Aisle.SPICES, aisle("Salt"));
        assertEquals(Aisle.OTHER, aisle("Ice"));
        assertEquals(Aisle.OTHER, aisle("Bamboo skewers"));
    }
}
