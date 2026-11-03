# DESIGN DOCUMENT - Week 10 additions (paste into the Word template; fix [FILL] items)
Formatting reminders (lost points in Week 8!): Calibri 11 body; use Heading 1/2 styles already in the template;
"Table N: ..." caption ABOVE each table; "Figure N: ..." caption BELOW each figure; numbering continues from
the last table/figure of Week 9; code in Consolas 9-10pt, under ~20 lines; body left-aligned/justified, 1.15-1.5 spacing.
Also: Revision History -> fill row 0.3 "Week 10 sections added" with date and author names. Update Table of Contents (F9).

## 5. API Integration Design   (author: Nesley)
Plate N Platter consumes the free public API TheMealDB (https://www.themealdb.com/api.php), which needs no sign-up
when using the shared development key "1".

Endpoint called (Table N below): GET https://www.themealdb.com/api/json/v1/1/search.php?s={name}

Table N: Endpoint used by MealDbClient
| Item | Value |
| Method | GET |
| URL | https://www.themealdb.com/api/json/v1/1/search.php?s={name} |
| Example | .../search.php?s=chicken |
| Authentication | None (shared test key "1" is in the URL path) |

Shape of the JSON response: an object with one key, "meals", holding an array of meal objects (or null when nothing matches).
Fields we use: idMeal, strMeal (name), strCategory, strArea, strInstructions, and strIngredient1 ... strIngredient20.
Short sample (keep under 20 lines, Consolas):
    { "meals": [ { "idMeal": "52772", "strMeal": "Teriyaki Chicken Casserole",
        "strCategory": "Chicken", "strArea": "Japanese",
        "strInstructions": "...", "strIngredient1": "soy sauce", ... } ] }

How the data is parsed and used: MealDbClient.searchByName() sends the request with Java's HttpClient, then
MealDbClient.parseMeals() reads the JSON with Gson and builds a MealData object for each meal; empty or null
ingredient fields are skipped. RecipesScreen shows the MealData list; when the user presses "Add to plan" the chosen
meal's name, category and date are saved to the database [FILL: exact class/method] so it appears on the Calendar and in
the analytics. The network call runs inside a background Task so the window never freezes (see Section 6).
Error handling: [FILL: what the user sees when offline or when no meals match].

## 6. Concurrency Design   (author: Yvonne)
Long-running operations on a background thread:
1. Bulk CSV import (CsvImportTask) - reads a large meal-plan history file (3,000+ rows in our sample) and inserts each valid row into SQLite.
2. TheMealDB search - a network call that can take time (RecipesScreen) [FILL: confirm].

Threading approach: both operations extend/use javafx.concurrent.Task and are started with new Thread(task).start().
CsvImportTask.call() runs off the JavaFX Application Thread; it reports progress with updateProgress() and updateMessage()
(throttled to every 50 rows so the UI thread is not flooded). Bad rows are skipped and counted instead of stopping the import.
A Cancel button calls task.cancel().

How the UI stays responsive: the ProgressBar is bound to task.progressProperty() and the status Label to
task.messageProperty(), so the JavaFX thread only redraws small updates. While an import runs, the user can switch to
other tabs and click other controls [FILL: say what you tested]. onSucceeded / onFailed handlers run back on the UI thread
to show the final result or an error message.
Table N: Background operations   | Operation | Class | Trigger | UI feedback |   [FILL rows]
Figure N: ProgressBar during CSV import   [screenshot]

## 7. Data Mining & Visualization Design   (author: Ken)
Mining step: MealPlanAnalyzer performs a frequency analysis over the planned meals stored in SQLite.
The DAO returns the category of every planned meal; MealPlanAnalyzer.countByCategory() builds a Map<String,Integer>
(category -> number of meals), mostFrequent() finds the most-planned category, and averagePrepMinutes() computes the average
preparation time. The input is read from the database each time, so changing the data changes the result.

Chart: a JavaFX BarChart (AnalyticsChart.build) titled "Meals Planned per Category", x-axis "Recipe category",
y-axis "Number of meals planned", one series named "Meals planned" with legend.
Why a bar chart: the data compares counts across separate, unordered categories (Breakfast, MainCourse, Dessert);
bars make the differences easy to compare, whereas a line chart would wrongly suggest a trend between categories.

Proof it is not hard-coded: [FILL: describe the test - imported meal_history_small.csv, then added N Dessert rows, and the Dessert bar grew from X to Y].
Figure N: Meals Planned per Category chart   [screenshot]
Table N: Mining result for the sample data   | Category | Count |   [FILL]

## 8. Testing Summary (if your instructor wants it now; Week 11 final)
Table N: Key JUnit tests | Class | Test | Edge case |
- CsvRecipeParserTest | wrongFieldCountIsRejected, nonNumericOrNegativePrepIsRejected, emptyLineAndBadDateAreRejected, keepsCommaInsideQuotes
- MealPlanAnalyzerTest | emptyListGivesEmptyMapAndNullMostFrequent, blankAndNullCategoriesBecomeUncategorized, changingInputChangesMostFrequent
- MealDbClientTest | nullMealsGivesEmptyList (API returns {"meals":null})
