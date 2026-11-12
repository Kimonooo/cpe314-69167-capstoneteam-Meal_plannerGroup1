# Known Issues and Limitations

This list matches Appendix C of the Design Document.

| # | Known issue or limit | Where |
|---|---|---|
| 1 | A planned meal cannot be edited. To change it, remove it and add it again. | `MealPlanScreen` |
| 2 | Importing the same CSV file twice adds the meals twice (there is no duplicate check). | `CsvImportTask` |
| 3 | TheMealDB gives no calories, so the user types them the first time a recipe is planned. The automatic Healthy Level is a simple budget rule, not nutrition advice. | `RecipesScreen`, `HealthClassifier` |
| 4 | The user types the prep time. The app only suggests one, estimated from the text of the steps: recipes that give no times only get the 2-minutes-per-ingredient part, and "1/2 hour" is read as 2 hours. The suggestion is used only when the user presses "Use N min". | `PrepTimeEstimator` |
| 5 | Recipe search and today's recommendations need the internet. Offline, a red message is shown and saved meals keep working. | `RecipesScreen`, `MealDbClient` |
| 6 | A security question is weaker than an e-mail reset, and there is no lock-out after many wrong answers. Answers are stored only as hashes and are not case-sensitive. | `AuthService` |
| 7 | Nested recipes exist only in memory (shown by the tests); `saved_recipes` has no table for sub-recipes. | `RecipeCalculator` |
| 8 | `UndoStack` and `CookingQueue` are shown by their tests; the pages use maps and lists. | `UndoStack`, `CookingQueue` |
| 9 | Only TheMealDB's free test key `1` is used, which is allowed for development and class use only. | `MealDbClient` |
| 10 | `plate_n_platter.db` is opened by a relative path, so it is created in the folder the app is started from. An older database file from before the 5-table model is not upgraded; import the CSV files again. | `DatabaseConnector` |
| 11 | The streak counts days with a planned meal, not days the meal was actually eaten. | `StreakCalculator` |
| 12 | The import reads the whole CSV file into memory (fine for tens of thousands of rows, not millions). | `CsvImportTask` |
| 13 | The database runs in WAL mode with `synchronous = NORMAL`. A power cut can lose the last save, but it cannot corrupt the database file. | `DatabaseConnector` |
