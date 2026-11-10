# The 9 mandatory requirements -> where they live (Kimberly finalizes; fix names to match the repo)
| # | Requirement | Class / file | Notes |
|---|---|---|---|
| 1 | OOD (abstract + 2+ subclasses, SRP) | model/Recipe, BreakfastRecipe, MainCourseRecipe, DessertRecipe | Used polymorphically in [FILL: where] |
| 2 | Data structure + recursion | util/CookingQueue (Queue), util/UndoStack (Stack), util/MealPlanAnalyzer (Map), util/RecipeCalculator (recursion) | Be ready to trace RecipeCalculator by hand for a small input |
| 3 | Automated testing (6+ JUnit 5) | src/test/java/... (see Testing Summary) | Each class has an edge case |
| 4 | Data modeling + database | resources/schema.sql, db/Database, db/UserDao, db/MealPlanHistoryDao [FILL] | PreparedStatement everywhere |
| 5 | API consumption | api/MealDbClient, api/MealData | JSON -> MealData -> shown in RecipesScreen |
| 6 | Event-driven GUI (3+ controls) | ui/RegisterScreen, CalendarScreen, RecipesScreen [FILL], ImportScreen [FILL], AnalyticsScreen [FILL] | Buttons: Create Account, Search, Add to plan, Import, Refresh |
| 7 | Concurrency | ui/CsvImportTask (Task on background Thread); search Task in RecipesScreen [FILL] | ProgressBar bound to task |
| 8 | Data mining | util/MealPlanAnalyzer (+ DAO query) | Counts per category, most frequent, average prep |
| 9 | Data visualization | ui/AnalyticsChart (BarChart) | Title, axis labels, legend |
