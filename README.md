# CPE314-Group1-H-Final-Project
## Project Information
**Project Title:** Plate n Platter

**Course:** CPE 314 — Software Design

**Section:** 3A

**Team Number:** 01

## Team Members

| No. | Name | Student ID Number | GitHub Username |
|:---:|------|:-----------------:|-----------------|
| 1 | Kimberly M. Luistro | 24211351 | [@luistrokimberly1-lchf](https://github.com/luistrokimberly1-lchf) |
| 2 | Nesley Cate S. Librea | 25237132 | [@libreanesleycate](https://github.com/libreanesleycate) |
| 3 | Venice M. Nadela | 25243908 | [@nadelavenice](https://github.com/nadelavenice) |
| 4 | Yvonne Mae M. Tapan | 25244906 | [@yvonnetapan](https://github.com/yvonnetapan) |
| 5 | Kirk Ryan P. Quisagan | 25221870 | [@KIRKQUISAGAN](https://github.com/KIRKQUISAGAN) |
| 6 | Kenneth V. Tamidles | 24218117 | [@Kimonooo](https://github.com/Kimonooo) |

## Requirement-to-code map

| # | Requirement | Where it is in the code | What to show |
|---|---|---|---|
| 1 | Object-oriented design | `model/SavedRecipe` (abstract) with `BreakfastRecipe`, `MainCourseRecipe`, `DessertRecipe`; interface `util/PlanStore` with `db/MealPlanDao` and `util/InMemoryPlanStore` | Same call `getPrepMinutes()`, a different rule in each subclass |
| 2 | Data structure and recursion | Queue: `util/CookingQueue`. Stack: `util/UndoStack`. Recursion: `util/RecipeCalculator.totalPrepMinutes` and `totalCalories` (base case: a recipe with no sub-recipes); `SavedRecipe.addSubRecipe` refuses a cycle with `CircularRecipeException` | Run `RecipeCalculatorTest`, `CookingQueueTest`, `UndoStackTest` |
| 3 | Automated testing (JUnit 5) | `src/test/java` — 30 test classes, 173 tests | Run `mvn clean test` |
| 4 | Data modeling and database | `resources/schema.sql` (5 tables), `db/DatabaseConnector`, `db/UserDao`, `db/MealPlanDao` (prepared statements; every query is filtered by `user_id`) | Two accounts see different meals |
| 5 | API consumption | `api/MealDbClient` (`search.php`, `filter.php`, `lookup.php`), parsed with Gson into `api/MealData`; used by the Recipes search, `service/RecommendationService` and "Look up online" on the Shopping List | Search a recipe; turn the Wi-Fi off and search again |
| 6 | Event-driven GUI (3+ controls) | `ui/` — `LoginScreen`, `RegisterScreen`, `ForgotPasswordScreen`, `TodayScreen`, `MealPlanScreen`, `RecipesScreen`, `ShoppingListScreen`, `ImportScreen`, `AnalyticsScreen`, `AccountScreen` in `AppShell` | Search, add to plan, calendar days, ticks, Import, Cancel |
| 7 | Concurrency | Every slow job is a JavaFX `Task` started by `AppShell.runInBackground`; `ui/CsvImportTask` imports with a progress bar and Cancel; the header clock is a `Timeline` | Import `sample_data/meal_history_30000.csv` and click other pages while it runs |
| 8 | Data mining | `service/StatisticsService` (`countByDay`, `countByWeekday`, `totals`, `healthyShare`, `averageCalories`), `util/StreakCalculator`, `util/HealthClassifier` | Add meals; the counts change |
| 9 | Data visualization | `ui/AnalyticsChart.buildGrouped` (grouped bar chart with title, axis labels and legend), `util/ChartMath` | The chart before and after adding meals |


