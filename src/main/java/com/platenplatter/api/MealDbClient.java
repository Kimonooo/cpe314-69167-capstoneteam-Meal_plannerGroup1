package com.platenplatter.api;

import com.google.gson.Gson;
import com.platenplatter.model.IngredientAmount;
import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Talks to TheMealDB (https://www.themealdb.com/api.php) with the free test key "1".
 *   searchByName   search.php?s=   the search bar on Recipes
 *   listByCategory filter.php?c=   Today's recommendations (gives only the id and the name)
 *   lookupById     lookup.php?i=   the full recipe after a recommendation is clicked
 * Gson reads the JSON straight into a small record; no JsonElement is used.
 * Call these methods on a background thread: they wait for the internet.
 */
public class MealDbClient {
    public static final String BASE_URL = "https://www.themealdb.com/api/json/v1/1/";

    /** The JSON is one object with a "meals" list; each meal is a flat object of strings. */
    private record MealDbResponse(List<Map<String, String>> meals) { }

    private static final Gson GSON = new Gson();

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))             // no internet: give up after 5 s
            .build();
    private final String baseUrl;

    public MealDbClient() {
        this(BASE_URL);
    }

    public MealDbClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public List<MealData> searchByName(String name) throws IOException, InterruptedException {
        return name == null || name.isBlank() ? List.of() : get("search.php?s=" + encode(name));
    }

    public List<MealData> listByCategory(String category) throws IOException, InterruptedException {
        return get("filter.php?c=" + encode(category));
    }

    public Optional<MealData> lookupById(String mealDbId) throws IOException, InterruptedException {
        return get("lookup.php?i=" + encode(mealDbId)).stream().findFirst();
    }

    /** Only a meal with exactly this name (and some ingredients), so a different dish is never used. */
    public Optional<MealData> findExact(String name) throws IOException, InterruptedException {
        return exactMatch(searchByName(name), name);
    }

    /** The first result with exactly this name (upper/lower case ignored) that has ingredients. */
    public static Optional<MealData> exactMatch(List<MealData> results, String name) {
        String wanted = name == null ? "" : name.trim();
        return results.stream()
                .filter(meal -> meal.name().equalsIgnoreCase(wanted) && !meal.ingredients().isEmpty())
                .findFirst();
    }

    private List<MealData> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(10))                  // no answer in 10 s: give up
                .header("User-Agent", "PlateNPlatter/1.0")
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new IOException("TheMealDB answered with HTTP " + response.statusCode());
        }
        return parseMeals(response.body());
    }

    public static List<MealData> parseMeals(String json) {
        MealDbResponse response = GSON.fromJson(json, MealDbResponse.class);
        if (response == null || response.meals() == null) {
            return List.of();                       // {"meals":null}: nothing matched
        }
        return response.meals().stream().map(MealDbClient::toMealData).toList();
    }

    private static MealData toMealData(Map<String, String> m) {
        List<IngredientAmount> amounts = IntStream.rangeClosed(1, 20)
                .filter(i -> !text(m, "strIngredient" + i).isBlank())  // skip blanks
                .mapToObj(i -> new IngredientAmount(text(m, "strIngredient" + i),
                                                    text(m, "strMeasure" + i)))
                .toList();
        return new MealData(text(m, "idMeal"), text(m, "strMeal"),
                text(m, "strCategory"), text(m, "strArea"),
                text(m, "strInstructions"), amounts);
    }

    /** The trimmed value, or "" when the field is missing or null. */
    private static String text(Map<String, String> m, String key) {
        return Objects.requireNonNullElse(m.get(key), "").trim();
    }

    /** True when the error means "no internet" (not "TheMealDB is down"). */
    public static boolean isOffline(Throwable error) {
        return Stream.iterate(error, Objects::nonNull, Throwable::getCause)   // the error and its causes
                .anyMatch(e -> e instanceof ConnectException
                        || e instanceof UnknownHostException
                        || e instanceof HttpConnectTimeoutException);
    }

    private static String encode(String text) {
        return URLEncoder.encode(text == null ? "" : text.trim(), StandardCharsets.UTF_8);
    }
}
