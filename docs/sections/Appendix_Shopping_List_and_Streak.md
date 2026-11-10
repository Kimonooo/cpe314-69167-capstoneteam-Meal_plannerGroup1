# Appendix A - Shopping list, calendar and streak   (author: Kirk)
Paste this as "Appendix A" at the END of the Design Document (after the Appendix heading). Put your own screenshots under it.
The Option H theme asks for "a shopping list generated from planned recipes", so this feature is part of the project even though Week 10 has no section for it.

## A.1 What the shopping list does
The user picks the days (Today, Next 7 days, This week, This month, or two dates). `ShoppingPlanner.plan()` reads every meal planned in that range from the database and finds each recipe's ingredients. Ingredients come from two places, in this order: (1) the list saved when the recipe was added from the Recipes tab (TheMealDB), (2) a built-in list for our sample dishes (`SampleIngredients`). A recipe with neither is listed as "no ingredient list yet", and the **Look up online** button asks TheMealDB for it.

## A.2 How the list is organised
`ShoppingListBuilder.build()` does three things:
1. **Cleans names:** "Eggs", " egg " and "EGG" become one item (`IngredientCategorizer.normalize`).
2. **Merges and counts:** the same ingredient from different meals becomes ONE line with "for N meals" (a recipe planned twice counts twice; an ingredient written twice in one recipe counts once).
3. **Groups by shop aisle:** Fruits & Vegetables, Meat & Seafood, Dairy & Eggs, Bakery, Pantry & Grains, Sauces & Condiments, Spices & Seasonings, Other. Each aisle is sorted A to Z.

`IngredientCategorizer.aisleFor()` checks an ordered list of rules and the first match wins. For example, "chicken stock" is a pantry item (the stock rule is before the chicken rule), "tomato paste" is a sauce and "eggplant" is a vegetable, not an egg.

On screen the user ticks items as they are bought (ticks are remembered, the text is crossed out and a progress bar shows "4 of 40 bought"). The list can be copied or saved as a text file.

Table N: Shopping list classes

| Class | Job |
|---|---|
| `IngredientCategorizer` | clean a name, choose the aisle |
| `ShoppingListBuilder` | merge, count, group, sort (pure logic, no database) |
| `ShoppingPlanner` | read the meals of a date range and their ingredients |
| `SampleIngredients` | built-in ingredients of the sample dishes |
| `IngredientLookup` | "Look up online" through TheMealDB |
| `ShoppingListScreen` | the tab |

## A.3 Calendar, today page and streak
- **Meal Plan tab:** a month calendar like the Milestone 1 one. Gold days have meals, a maroon outline marks today, and clicking a day shows its meals, where a meal can be removed or a new one added (`MealPlanScreen`, `CalendarMath`).
- **Header:** the real-time date and clock (updated every second) and the streak badge (`HeaderBar`, `StreakCalculator`).
- **Today tab:** current streak, best streak, meals today, and the 7 days of the week (`TodayScreen`).
- **Streak rule:** the number of days in a row, ending today, that have at least one planned meal. If today has no meal yet but yesterday did, the streak is still alive and the header reminds the user to plan a meal.

Figure N: Shopping List tab    <- screenshot, caption goes BELOW
Figure N: Meal Plan calendar    <- screenshot, caption goes BELOW
Figure N: Today tab with the streak    <- screenshot, caption goes BELOW

## A.4 Tests
`IngredientCategorizerTest` (aisle rules and tricky names), `ShoppingListBuilderTest` (merging, counting, order, missing recipes, plain text), `ShoppingPlannerTest`, `SampleIngredientsTest` (every dish in the sample CSV files has ingredients), `IngredientLookupTest`, `StreakCalculatorTest`, `CalendarMathTest`, `DateTimeTextTest`.
