# The 9 mandatory requirements -> where they live (final version, Kirk)
Open this next to the code in the IDE for the demo. If a file name is different in your repo, fix it here.

| # | Requirement | Where it lives | What to say / show |
|---|---|---|---|
| 1 | Object-oriented design | `model/Recipe` (abstract) and `BreakfastRecipe`, `MainCourseRecipe`, `DessertRecipe`; used through a `Recipe` reference in `RecipeDemo` and the cooking queue | Same call `getPrepMinutes()`, a different rule in each subclass |
| 2 | Data structure + recursion | Queue: `util/CookingQueue`. Stack: `util/UndoStack`. Map: `util/MealPlanBook`, and the `TreeMap` in `util/MealPlanAnalyzer`. Recursion: `util/RecipeCalculator` (base case = a recipe with no sub-recipes) | Trace `RecipeCalculator` by hand for a small recipe |
| 3 | Automated testing (6+ JUnit 5) | `src/test/java/...` about 100 tests, with edge cases in every test class | Run `mvn clean test` live |
| 4 | Data modeling + database | `resources/schema.sql`, `db/MealPlanHistoryDao`, `db/DatabaseConnector`, `db/UserDao`, tables `meal_history` and `meal_ingredient` | Show the file and one live query; point at the `?` in `PreparedStatement` |
| 5 | API consumption | `api/MealDbClient`, `api/MealData`; screens `RecipesScreen` and the "Look up online" button in `ShoppingListScreen` | Turn the Wi-Fi off and on |
| 6 | Event-driven GUI (3+ controls) | `ui/RecipesScreen`, `MealPlanScreen`, `ShoppingListScreen`, `TodayScreen`, `ImportScreen`, `AnalyticsScreen`, `AppShell` | Search, Add to plan, calendar days, ticks, Import, Cancel, Refresh |
| 7 | Concurrency | `ui/CsvImportTask` (background Thread), and a `Task` in every screen; the live clock is a `Timeline` | Import `meal_history_30000.csv` and click another tab while it runs |
| 8 | Data mining | `util/MealPlanAnalyzer` (counts per category, most frequent, average); `util/StreakCalculator` (streak by date) | Add meals, press Refresh: the numbers change |
| 9 | Data visualization | `ui/AnalyticsChart` (BarChart with title, axis labels, legend), `util/ChartMath` | Show the chart before and after changing the data |
