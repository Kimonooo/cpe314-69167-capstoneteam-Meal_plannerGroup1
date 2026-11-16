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

## How to run

You need Java 17 or newer and Maven (Eclipse's built-in Maven works too). TheMealDB search, today's
picks and the food photos need the internet; everything else works offline.

```
git clone https://github.com/Kimonooo/cpe314-69167-capstoneteam-Meal_plannerGroup1.git
cd cpe314-69167-capstoneteam-Meal_plannerGroup1
mvn clean test        # 248 tests in 40 test classes, 0 failures
mvn javafx:run        # starts the app (main class com.platenplatter.PlateNPlatterApp)
```

In Eclipse: File > Import > Existing Maven Projects, then right-click the project >
Run As > Maven build... > Goals: `javafx:run`.

1. There is no default account. Click **Create account** on the Log in page and sign up
   (username: 3 to 20 letters, numbers, dots or underscores; password: 6 or more characters).
2. The app makes `plate_n_platter.db` in the folder it is started from.
3. To load sample meals: **Import** > Choose CSV... > `sample_data/meal_history_demo.csv` > Import.
   `meal_history_bad_rows.csv` shows how bad rows are skipped; `meal_history_30000.csv` shows the
   background import (import it only once: the second time every row is "already planned").

## How the code is organized

| Package | What is in it |
|---|---|
| `model` | The data objects: `SavedRecipe` (abstract) and its 3 subclasses, `PlannedMeal`, `User`, enums |
| `db` | `schema.sql` runner, `UserDao`, `MealPlanDao` (one lock on the shared connection), the `PlanStore` interface and `InMemoryPlanStore` |
| `api` | `MealDbClient` (through the `HttpSender` interface), `MealData` (DTO), `RecipeMapper`, `MeasureParser` |
| `service` | The rules: accounts and lock-out, statistics, streak, healthy level, shopping list, CSV parser and importer, recursion |
| `controller` | One controller per page; the pages call only their controller |
| `ui` | The JavaFX pages, the theme and the background worker pool |
| `util` | Small helpers: dates and text, chart math, calendar math, `CookingQueue`, `UndoStack`, `DataChangeNotifier` |

`PlateNPlatterApp` is the only place that opens the database and creates the DAOs, services and controllers.

## Requirement-to-code map

| # | Requirement | Where it is in the code | What to show |
|---|---|---|---|
| 1 | Object-oriented design | `model/SavedRecipe` (abstract) with `BreakfastRecipe`, `MainCourseRecipe`, `DessertRecipe`; interface `db/PlanStore` with `db/MealPlanDao` and `db/InMemoryPlanStore` | The suggested prep time differs by meal type (`RecipeController.open`) |
| 2 | Data structure and recursion | Queue: `util/CookingQueue` (Meal Plan's cooking order). Stack: `util/UndoStack` (Undo for a removed meal). Both used by `controller/MealPlanController`. Recursion: `service/RecipeCalculator.totalOf` (the day's totals: day > meal slots > meals) and `SavedRecipe.contains` (refuses a cycle). Map: `ShoppingList`, `StatisticsService` | Meal Plan: Done, next; Remove then Undo; the day total |
| 3 | Automated testing (JUnit 5) | `src/test/java` — 40 test classes, 248 tests; saved TheMealDB answers in `src/test/resources/mealdb` | Run `mvn clean test` |
| 4 | Data modeling and database | `resources/schema.sql` (5 tables), `db/DatabaseConnector`, `db/UserDao`, `db/MealPlanDao` (prepared statements; every query is filtered by `user_id`; an import is one transaction) | Two accounts see different meals |
| 5 | API consumption | `api/MealDbClient` (`search.php`, `filter.php`, `lookup.php`) parsed with Gson into `api/MealData`; `api/RecipeMapper`; used by `RecipeController`, `RecommendationService`, `ShoppingListController` and `ui/MealPhotos` | Search a recipe; turn the Wi-Fi off and search again |
| 6 | Event-driven GUI (3+ controls) | `ui/` pages, each calling one controller: `RecipesScreen` > `RecipeController`, `MealPlanScreen` > `MealPlanController`, `ImportScreen` > `ImportController` ... | Search, Add to my plan, calendar days, Undo, Import, Cancel |
| 7 | Concurrency | One named worker pool (`AppShell.newWorkers`, shut down on close); every slow job is a JavaFX `Task`; `ui/CsvImportTask` > `service/MealImporter` with progress and Cancel; the header clock is a `Timeline` | Import `sample_data/meal_history_30000.csv` and click other pages while it runs |
| 8 | Data mining | `service/StatisticsService` (`countByDay`, `countByWeekday`, `totals`, `healthyShare`, `averageCalories`) through `controller/AnalyticsController`, `service/StreakCalculator`, `service/HealthClassifier` | Add meals; the counts change |
| 9 | Data visualization | `ui/AnalyticsChart.buildGrouped` (grouped bar chart with title, axis labels and legend), `util/ChartMath` | The chart before and after adding meals |

See `docs/KNOWN_ISSUES.md` for the known limits.
