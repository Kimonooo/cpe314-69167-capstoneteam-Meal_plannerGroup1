package com.platenplatter.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.FoodGroup;
import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.net.http.HttpConnectTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;

class MealDbClientTest {

    /** A trimmed copy of the real search.php answer for "Arrabiata", with blank and null slots. */
    private static final String ARRABIATA = """
            {"meals":[{"idMeal":"52771","strMeal":"Spicy Arrabiata Penne","strCategory":"Vegetarian",
            "strArea":"Italian","strInstructions":"Bring a large pot of water to a boil.",
            "strMealThumb":"https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg",
            "strIngredient1":"penne rigate","strMeasure1":"1 pound",
            "strIngredient2":"olive oil","strMeasure2":"1/4 cup",
            "strIngredient3":"","strMeasure3":" ",
            "strIngredient4":"garlic","strMeasure4":"3 cloves",
            "strIngredient5":null,"strMeasure5":null,"dateModified":null}]}""";

    @Test
    void aSearchResultIsReadIntoMealData() {
        List<MealData> meals = MealDbClient.parseMeals(ARRABIATA);
        assertEquals(1, meals.size());
        MealData penne = meals.get(0);
        assertEquals("52771", penne.mealDbId());
        assertEquals("Spicy Arrabiata Penne", penne.name());
        assertEquals("Vegetarian", penne.category());
        assertEquals("Italian", penne.area());
        assertFalse(penne.isSummary());
    }

    @Test
    void eachMeasureStaysWithItsIngredientWhenSlotsAreBlank() {
        MealData penne = MealDbClient.parseMeals(ARRABIATA).get(0);
        assertEquals(List.of("1 pound penne rigate", "1/4 cup olive oil", "3 cloves garlic"),
                penne.ingredients().stream().map(Object::toString).toList());
        assertEquals(FoodGroup.FRUITS_VEGETABLES, penne.ingredients().get(2).getIngredient().getFoodGroup());
    }

    @Test
    void thePhotoAddressIsReadFromStrMealThumbAndAMissingOneIsEmpty() {
        MealData penne = MealDbClient.parseMeals(ARRABIATA).get(0);
        assertEquals("https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg", penne.thumbnail());
        assertTrue(penne.hasPhoto());
        MealData plain = MealDbClient.parseMeals("{\"meals\":[{\"strMeal\":\"Lugaw\",\"idMeal\":\"1\"}]}").get(0);
        assertEquals("", plain.thumbnail());
        assertFalse(plain.hasPhoto());
        assertFalse(new MealData("2", "Typed by hand", "", "", "", List.of()).hasPhoto());
    }

    @Test
    void noMatchGivesAnEmptyListNotACrash() {
        assertTrue(MealDbClient.parseMeals("{\"meals\":null}").isEmpty());
        assertTrue(MealDbClient.parseMeals("{}").isEmpty());
        assertTrue(MealDbClient.parseMeals("").isEmpty());
    }

    @Test
    void aFilterResultOnlyHasTheIdAndTheName() {
        List<MealData> meals = MealDbClient.parseMeals("""
                {"meals":[{"strMeal":"Ratatouille","strMealThumb":"x.jpg","idMeal":"52908"},
                          {"strMeal":"Shakshuka","strMealThumb":"y.jpg","idMeal":"52963"}]}""");
        assertEquals(List.of("Ratatouille", "Shakshuka"), meals.stream().map(MealData::name).toList());
        assertEquals("", meals.get(0).category());
        assertTrue(meals.get(0).isSummary());
    }

    @Test
    void lookUpOnlineOnlyUsesAnExactNameMatchWithIngredients() {
        MealData penne = MealDbClient.parseMeals(ARRABIATA).get(0);
        MealData other = new MealData("1", "Penne Pasta Bake", "Pasta", "", "", penne.ingredients());
        MealData empty = new MealData("2", "Spicy Arrabiata Penne", "", "", "", List.of());
        assertEquals("52771", MealDbClient.exactMatch(List.of(other, empty, penne), " spicy arrabiata PENNE ").orElseThrow().mealDbId());
        assertTrue(MealDbClient.exactMatch(List.of(other), "Spicy Arrabiata Penne").isEmpty());   // a different dish is never used
        assertTrue(MealDbClient.exactMatch(List.of(), "Adobo").isEmpty());
    }

    @Test
    void noInternetIsToldApartFromAServerError() {
        assertTrue(MealDbClient.isOffline(new ConnectException("Connection refused")));
        assertTrue(MealDbClient.isOffline(new UnknownHostException("www.themealdb.com")));
        assertTrue(MealDbClient.isOffline(new HttpConnectTimeoutException("timed out")));
        assertTrue(MealDbClient.isOffline(new IOException("wrapped", new ConnectException())));   // a cause counts too
        assertFalse(MealDbClient.isOffline(new IOException("TheMealDB answered with HTTP 500")));
        assertFalse(MealDbClient.isOffline(null));
    }
}
