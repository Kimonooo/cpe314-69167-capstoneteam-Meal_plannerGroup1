# Known Issues / Limitations (Venice keeps this honest; show it on request at the demo)
| # | Issue / limitation | Where | Workaround or plan |
|---|---|---|---|
| 1 | TheMealDB search and "Look up online" need internet; offline shows a friendly error | MealDbClient | Message tells the user to check the connection |
| 2 | TheMealDB gives no prep time, so the user picks it with a spinner (default 30 min) | RecipesScreen | Documented limitation |
| 3 | TheMealDB categories (Chicken, Beef...) are grouped into our 3 categories; everything that is not Breakfast or Dessert counts as MainCourse | CategoryMapper | The MainCourse bar can look large |
| 4 | The shopping list shows WHAT to buy and for how many meals, but not amounts (grams, cups) | ShoppingListBuilder | Future work |
| 5 | The aisle of an ingredient is chosen by keyword rules; an unusual ingredient can land in "Other" | IngredientCategorizer | Add a keyword to the rule list |
| 6 | Meals imported from a CSV have no ingredients; the built-in list covers only our sample dishes | SampleIngredients | "Look up online" fills in dishes TheMealDB knows |
| 7 | The import reads the whole CSV into memory (fine for tens of thousands of rows, not millions) | CsvImportTask | Could stream line by line |
| 8 | Bad CSV rows are skipped and counted | CsvRecipeParser | The skipped count is shown on screen |
| 9 | SQLite uses PRAGMA synchronous=OFF to speed up bulk import (small risk if the PC crashes mid-import) | MealPlanHistoryDao | Acceptable for course data |
| 10 | Planned meals can be removed but not edited | MealPlanScreen | Remove it and add it again |
| 11 | The streak counts days with a planned meal, not days when the meal was cooked | StreakCalculator | Future work: a "Mark as cooked" button |
| 12 | The Sign up and Calendar tabs from Milestone 1 are in the old App.java, not in the new main window | PlateNPlatterApp | Run `com.platenplatter.App` for them, or add the one-line `Milestone2Tabs` call |
| 13 | [FILL: real bugs found during QA] | | |
