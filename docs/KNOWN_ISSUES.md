# Known Issues and Limitations

This list matches Appendix C of the Design Document.

| # | Known issue or limit | Where |
|---|---|---|
| 1 | A planned meal cannot be edited. To change it, remove it and add it again (Remove can be undone with Undo while the app is open). | `MealPlanScreen`, `MealPlanController` |
| 2 | The Undo list lives in memory: it is empty again after a log-out or a restart. | `MealPlanController` |
| 3 | TheMealDB gives no calories, so the user types them the first time a recipe is planned. The automatic Healthy Level is a simple budget rule, not nutrition advice. | `RecipesScreen`, `HealthClassifier` |
| 4 | The user types the prep time. The app only suggests one, estimated from the text of the steps: recipes that give no times only get the 2-minutes-per-ingredient part, and "1/2 hour" is read as 2 hours. Prep time must be 0 to 1,440 minutes and calories 0 to 10,000. | `PrepTimeEstimator`, `RecipeController` |
| 5 | Recipe search, today's picks and the photos need the internet. Offline, a red message with Retry is shown and saved meals keep working. HTTP 429 (too many requests) shows "TheMealDB is busy". | `RecipesScreen`, `MealDbClient` |
| 6 | A security question is weaker than an e-mail reset. Five wrong passwords or answers in a row lock the username for five minutes; the lock is kept in memory, so a restart clears it. | `AuthService`, `LoginAttempts` |
| 7 | Sub-recipes exist only in memory (the Meal Plan day total is built as a tree each time); `saved_recipes` has no table for sub-recipes. | `RecipeCalculator`, `MealPlanController` |
| 8 | A meal counts as "already planned" when the same recipe name is on the same day in the same slot; a second, different portion of the same dish in one slot cannot be imported. | `MealPlanDao.saveMeals` |
| 9 | Only TheMealDB's free test key `1` is used, which is allowed for development and class use only. | `MealDbClient` |
| 10 | `plate_n_platter.db` is opened by a relative path, so it is created in the folder the app is started from. An older database file from before the 5-table model is not upgraded; import the CSV files again. The new CHECK limits (prep 0 to 1,440, calories 0 to 10,000) apply to new database files; the app checks the same limits for every file. | `DatabaseConnector`, `schema.sql` |
| 11 | The streak counts days with a planned meal, not days the meal was actually eaten. | `StreakCalculator` |
| 12 | The import reads the whole CSV file into memory and saves it as one transaction (fine for tens of thousands of rows, not millions). While it saves, other pages wait for the database lock and then show the new data. | `MealImporter`, `MealPlanDao` |
| 13 | The database runs in WAL mode with `synchronous = NORMAL`. A power cut can lose the last save, but it cannot corrupt the database file. | `DatabaseConnector` |
| 14 | The JavaFX pages have no automated tests; the logic they call (controllers, services, DAOs, API client) does. | `src/test/java` |
