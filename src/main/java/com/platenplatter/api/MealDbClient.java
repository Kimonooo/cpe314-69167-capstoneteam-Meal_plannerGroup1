package com.platenplatter.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Calls TheMealDB search endpoint and parses the JSON into MealData objects.
 * Responsibility: network + JSON parsing only (no GUI, no database).
 * IMPORTANT: call searchByName from a background thread, never the UI thread.
 */
public class MealDbClient {
    private static final String SEARCH_URL =
            "https://www.themealdb.com/api/json/v1/1/search.php?s=";

    private final HttpClient http = HttpClient.newHttpClient();

    public List<MealData> searchByName(String name) throws IOException, InterruptedException {
        if (name == null || name.isBlank()) {
            return new ArrayList<>();
        }
        String url = SEARCH_URL + URLEncoder.encode(name.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .header("User-Agent", "PlateNPlatter/1.0")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("TheMealDB returned HTTP " + response.statusCode());
        }
        return parseMeals(response.body());
    }

    /** Public + static so JUnit can test parsing without internet. */
    public static List<MealData> parseMeals(String json) {
        List<MealData> result = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        JsonElement mealsElement = root.get("meals");
        if (mealsElement == null || mealsElement.isJsonNull()) {
            return result; // TheMealDB sends {"meals":null} when nothing matches
        }
        JsonArray meals = mealsElement.getAsJsonArray();
        for (JsonElement e : meals) {
            JsonObject m = e.getAsJsonObject();
            List<String> ingredients = new ArrayList<>();
            for (int i = 1; i <= 20; i++) {
                String ing = text(m, "strIngredient" + i);
                if (!ing.isBlank()) {
                    ingredients.add(ing.trim());
                }
            }
            result.add(new MealData(
                    text(m, "idMeal"), text(m, "strMeal"), text(m, "strCategory"),
                    text(m, "strArea"), text(m, "strInstructions"), ingredients));
        }
        return result;
    }

    private static String text(JsonObject obj, String key) {
        JsonElement e = obj.get(key);
        return (e == null || e.isJsonNull()) ? "" : e.getAsString();
    }
}
