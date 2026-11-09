# Plate N Platter: Week 10 revision (design document version 0.4)

This update follows the revised Project Design Document (version 0.4). It was added in six turns on the
branch `feature/week10-revision`, one turn per member, then merged into `project/milestone-2`.

## Database: 8 tables became 5

| Table (plural) | Primary key (one) | Java class |
| --- | --- | --- |
| `users` | `user_id` | `model/User` |
| `saved_recipes` | `saved_recipe_id` | `model/SavedRecipe` (abstract) with `BreakfastRecipe`, `MainCourseRecipe`, `DessertRecipe` |
| `ingredients` | `ingredient_id` | `model/Ingredient` |
| `ingredient_amounts` | `ingredient_amount_id` | `model/IngredientAmount` |
| `planned_meals` | `planned_meal_id` | `model/PlannedMeal` |

- Every table has exactly one primary key; a foreign key has the same name as the key it points to.
- `meal_plan_entries` and `meal_history` were merged into `planned_meals`; `meal_ingredient` and
  `recipe_components` were removed; `recipes` became `saved_recipes`; `recipe_ingredients` became `ingredient_amounts`.
- The database file is now **`plate_n_platter.db`**. The old `platenplatter.db` is not read any more:
  sign up again and import the CSV files from `sample_data/` again.

## New features

- **Forgot password**: sign-up asks for one of four security questions; "Forgot password?" on Log in resets the
  password after the right answer. The answer is stored only as a salted PBKDF2 hash, ignoring case and spaces.
- **Recipe search bar with Today's recommendations**: nothing is listed until you search, apart from six picks for the
  time of day (Breakfast before 11:00, Lunch until 15:59, Snack until 17:59, then Dinner).
- **No internet message**: a red banner with a Retry button under the search bar.
- **Prep time is estimated** from the recipe's steps and ingredients (`PrepTimeEstimator`), and the **calories are
  remembered** from the last time the recipe was planned (`saved_recipes.usual_calories`).
- **Web-style look**: the team logo, rounded navigation pills instead of tabs, white cards, text-only recipe cards
  (no food photos) and a 250 ms fade-and-slide when the page changes.
- **Shopping list grouped by food group** (`FoodGroup`), with "Look up online" for recipes planned without ingredients.

## Simpler code

- 75 main classes became 57. Removed or merged: `App`, `CalendarScreen`, `Database`, `DatabaseDemo`, `UserDaoDemo`,
  `MealPlanEntry`, `MealPlanAnalyzer`, `MealPlanBook`, `Milestone2Preview`, `Milestone2Tabs`, `ImportAnalyticsPreview`,
  `RecipesScreenPreview`, `Loader`, `RowSink`, `ImportRow`, `ShoppingPlanner`, `ShoppingListBuilder`, `MealIngredients`,
  `SampleIngredients`, `IngredientCategorizer`, `IngredientLookup`, `StatFormat`, `TimeFormat`, `DateTimeText`.
- Streams instead of `for` and `while` loops. The only loop left is JDBC's `while (rs.next())` in `MealPlanDao.readAll`.
- Gson reads TheMealDB's JSON straight into a record; no `JsonElement` is used.
- One prep scale everywhere (`PrepTier`: Quick, Average, Slow).

## Tests

`mvn test` runs **173 tests in 30 test classes**. New: `PrepTimeEstimatorTest`, `RecommendationServiceTest`.
Renamed or merged: `MealPlanDaoTest` (was `MealPlanHistoryDaoTest`), `SavedRecipeTest` (was `RecipeTest`),
`FoodGroupTest` (was `IngredientCategorizerTest`), `ShoppingListTest` (was `ShoppingListBuilderTest` and
`ShoppingPlannerTest`), `TextFormatTest` (was `DateTimeTextTest`, `StatFormatTest` and `TimeFormatTest`),
`UserMealsTest` (was `UserMealsAndUpgradeTest`); `MealDbClientTest` now also covers the old `IngredientLookupTest`.
Removed with their classes: `MealPlanAnalyzerTest`, `MealPlanBookTest`, `SampleIngredientsTest`.
