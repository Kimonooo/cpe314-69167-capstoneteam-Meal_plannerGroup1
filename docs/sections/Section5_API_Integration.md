# Section 5 - API Integration Design   (author: Nesley)
How to use this file: read it, then replace every [YOUR TURN] with what YOU saw on screen. Send the finished text to Kirk.
Formatting reminders: Calibri 11, left-aligned or justified. Table captions go ABOVE a table ("Table N: ..."). Figure captions go BELOW a picture ("Figure N: ..."). Numbers continue from Week 9.

## 5. API Integration Design

Plate N Platter uses the free public API **TheMealDB** (https://www.themealdb.com/api.php). It needs no sign-up when we use the shared development key `1`. The app uses it to search for real recipes by name.

### 5.1 Endpoint called

Table N: Endpoint used by MealDbClient

| Item | Value |
|---|---|
| Method | GET |
| URL | `https://www.themealdb.com/api/json/v1/1/search.php?s={name}` |
| Example | `.../search.php?s=chicken` |
| Login needed | No (the shared key `1` is part of the URL) |
| Sent by | `MealDbClient.searchByName()` using Java's `HttpClient` |

The search word is URL-encoded, and the request sends a `User-Agent` header (`PlateNPlatter/1.0`). If the website answers with anything other than HTTP 200, `MealDbClient` throws an `IOException`.

### 5.2 Shape of the JSON answer

The answer is one JSON object with one key, `meals`. It holds an array of meal objects, or `null` when nothing matches.

```
{ "meals": [ { "idMeal": "52772", "strMeal": "Teriyaki Chicken Casserole",
    "strCategory": "Chicken", "strArea": "Japanese",
    "strInstructions": "Preheat oven to 350...",
    "strIngredient1": "soy sauce", "strIngredient2": "water", ... } ] }
```

Table N: JSON fields and where they go

| JSON field | Becomes (in `MealData`) |
|---|---|
| `idMeal` | `id` |
| `strMeal` | `name` |
| `strCategory` | `category` |
| `strArea` | `area` (the cuisine) |
| `strInstructions` | `instructions` |
| `strIngredient1` ... `strIngredient20` | `ingredients` (a list; blank or null entries are skipped). Saved so the shopping list can use them |

### 5.3 How the data is parsed and used

1. `MealDbClient.parseMeals()` reads the text with Gson (`JsonParser.parseString`) and builds one `MealData` object for each meal. If `meals` is `null` it returns an empty list, so "no results" never crashes the app. This method is `public static`, so JUnit can test it without the internet (`MealDbClientTest`).
2. `RecipesScreen` shows the `MealData` objects in a list (name, cuisine, and a colored category chip). When you click one, the right side shows its ingredients as chips and its cooking steps.
3. When the user presses **Add to my plan**, `CategoryMapper` turns TheMealDB's category (like "Chicken") into one of our three plan categories (Breakfast, MainCourse or Dessert). The app builds an `ImportRow` (date, name, category, prep minutes) and saves it with `MealPlanHistoryDao.insert()`. The meal's ingredient list (`strIngredient1` to `strIngredient20`) is saved in the same step in the table `meal_ingredient`, so the **Shopping List** tab can use it. The saved meal is also counted by the Analytics tab, shown on the Meal Plan calendar and counted in the streak.

### 5.4 Background thread and errors

The web call runs inside a `javafx.concurrent.Task` on a background thread, so the window never freezes while the website answers (see Section 6).

Table N: What the user sees

| Situation | Message on screen |
|---|---|
| Search box empty | "Please type something to search." |
| Website found nothing | "No recipes found for ... Try another word." |
| No internet | "Could not reach TheMealDB. Check your internet connection." |
| Recipe added | "Added ... to your plan on ..." |

[YOUR TURN: add one sentence about what you tested, for example: "We searched for chicken, adobo and zzzzqq, and also turned off the Wi-Fi."]

Figure N: Recipes tab showing search results    <- screenshot, caption goes BELOW
Figure N: Message after adding a recipe to the plan    <- screenshot, caption goes BELOW

### 5.5 Tests for this section
`MealDbClientTest` checks that one meal with ingredients is parsed correctly (blank and null ingredients are skipped) and that `{"meals":null}` gives an empty list (an edge case).

### 5.6 Second use of the API: "Look up online" in the Shopping List
Meals that were imported from a CSV file have no ingredient list. On the Shopping List tab, `IngredientLookup.fill()` calls the same endpoint (`search.php?s={name}`) for each such recipe. It saves the ingredients ONLY when the name of a result matches exactly (ignoring capital letters), so the app never attaches the wrong ingredients to a dish. The call runs in a background `Task`. `IngredientLookupTest` checks an exact match, a different dish, no results and a network error.
