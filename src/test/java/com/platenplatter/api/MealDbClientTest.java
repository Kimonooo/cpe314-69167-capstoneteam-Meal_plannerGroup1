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
            {"meals":[{"idMeal":"52771","strMeal":"Spicy Arrabiata Penne",
            "strCategory":"Vegetarian","strArea":"Italian",
            "strInstructions":"Bring a large pot of water to a boil.",
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
        assertEquals(FoodGroup.FRUITS_VEGETABLES, penne.ingredients().get(2).getIngredient()
                .getFoodGroup());
    }

    @Test
    void thePhotoAddressIsReadFromStrMealThumbAndAMissingOneIsEmpty() {
        MealData penne = MealDbClient.parseMeals(ARRABIATA).get(0);
        assertEquals("https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg",
                penne.thumbnail());
        assertTrue(penne.hasPhoto());
        MealData plain = MealDbClient.parseMeals(
                "{\"meals\":[{\"strMeal\":\"Lugaw\",\"idMeal\":\"1\"}]}").get(0);
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
        assertEquals(List.of("Ratatouille", "Shakshuka"),
                meals.stream().map(MealData::name).toList());
        assertEquals("", meals.get(0).category());
        assertTrue(meals.get(0).isSummary());
    }

    @Test
    void lookUpOnlineOnlyUsesAnExactNameMatchWithIngredients() {
        MealData penne = MealDbClient.parseMeals(ARRABIATA).get(0);
        MealData other = new MealData("1", "Penne Pasta Bake", "Pasta", "", "",
                penne.ingredients());
        MealData empty = new MealData("2", "Spicy Arrabiata Penne", "", "", "", List.of());
        assertEquals("52771", MealDbClient.exactMatch(List.of(other, empty, penne),
                " spicy arrabiata PENNE ").orElseThrow().mealDbId());
        // a different dish is never used
        assertTrue(MealDbClient.exactMatch(List.of(other), "Spicy Arrabiata Penne").isEmpty());
        assertTrue(MealDbClient.exactMatch(List.of(), "Adobo").isEmpty());
    }

    @Test
    void noInternetIsToldApartFromAServerError() {
        assertTrue(MealDbClient.isOffline(new ConnectException("Connection refused")));
        assertTrue(MealDbClient.isOffline(new UnknownHostException("www.themealdb.com")));
        assertTrue(MealDbClient.isOffline(new HttpConnectTimeoutException("timed out")));
        // a cause counts too
        assertTrue(MealDbClient.isOffline(new IOException("wrapped", new ConnectException())));
        assertFalse(MealDbClient.isOffline(new IOException("TheMealDB answered with HTTP 500")));
        assertFalse(MealDbClient.isOffline(null));
    }

    // ------------------------------------------------------------------ with a saved answer file

    @Test
    void aSavedSearchAnswerIsReadThroughTheFakeSender() throws Exception {
        MealDbClient client = new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.file("search_arrabiata.json"));
        ApiResult result = client.search("Arrabiata");
        assertEquals(ApiResult.Status.OK, result.status());
        MealData penne = result.meals().get(0);
        assertEquals("Spicy Arrabiata Penne", penne.name());
        assertEquals(8, penne.ingredients().size());           // slots 9 to 20 are blank or null
        assertEquals("1 tin", penne.ingredients().get(3).getMeasure());   // measures are trimmed
    }

    @Test
    void theAddressIsTheBasePlusTheEncodedName() throws Exception {
        FakeSender sender = FakeSender.file("no_match.json");
        new MealDbClient("https://example.test/api/", sender).search(" chicken adobo ");
        assertEquals("https://example.test/api/search.php?s=chicken+adobo",
                sender.asked().get(0).toString());
    }

    @Test
    void noMatchIsEmptyNotFailed() throws Exception {
        ApiResult result = new MealDbClient(MealDbClient.BASE_URL, FakeSender.file("no_match.json"))
                .search("zzzz");
        assertEquals(ApiResult.Status.EMPTY, result.status());
        assertTrue(result.meals().isEmpty());
    }

    @Test
    void aServerErrorIsFailedWithAnIoException() throws Exception {
        ApiResult result = new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.status(500)).search("adobo");
        assertEquals(ApiResult.Status.FAILED, result.status());
        assertTrue(result.error() instanceof IOException);
        assertFalse(MealDbClient.isOffline(result.error()));
        assertFalse(MealDbClient.isRateLimited(result.error()));
    }

    @Test
    void tooManyRequestsIsToldApart() throws Exception {
        ApiResult result = new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.status(MealDbClient.TOO_MANY_REQUESTS)).search("adobo");
        assertTrue(result.isFailed());
        assertTrue(MealDbClient.isRateLimited(result.error()));
    }

    @Test
    void noInternetIsFailedAndOffline() throws Exception {
        ApiResult result = new MealDbClient(MealDbClient.BASE_URL,
                FakeSender.offline()).search("adobo");
        assertTrue(result.isFailed());
        assertTrue(MealDbClient.isOffline(result.error()));
    }

    @Test
    void aBlankSearchDoesNotCallTheInternet() throws Exception {
        FakeSender sender = FakeSender.offline();
        assertEquals(ApiResult.Status.EMPTY, new MealDbClient(MealDbClient.BASE_URL,
                sender).search("  ").status());
        assertTrue(sender.asked().isEmpty());
    }
}
