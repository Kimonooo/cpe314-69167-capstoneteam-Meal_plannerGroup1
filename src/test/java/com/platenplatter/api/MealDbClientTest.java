package com.platenplatter.api;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class MealDbClientTest {

    @Test
    void parsesOneMealWithIngredients() {
        String json = "{\"meals\":[{\"idMeal\":\"1\",\"strMeal\":\"Chicken Adobo\","
                + "\"strCategory\":\"Chicken\",\"strArea\":\"Filipino\","
                + "\"strInstructions\":\"Simmer.\",\"strIngredient1\":\"Chicken\","
                + "\"strIngredient2\":\"Soy sauce\",\"strIngredient3\":\"\","
                + "\"strIngredient4\":null}]}";
        List<MealData> meals = MealDbClient.parseMeals(json);
        assertEquals(1, meals.size());
        assertEquals("Chicken Adobo", meals.get(0).getName());
        assertEquals("Chicken", meals.get(0).getCategory());
        assertEquals(2, meals.get(0).getIngredients().size()); // blank + null skipped
    }

    @Test
    void nullMealsGivesEmptyList() {
        assertTrue(MealDbClient.parseMeals("{\"meals\":null}").isEmpty());
    }
}
