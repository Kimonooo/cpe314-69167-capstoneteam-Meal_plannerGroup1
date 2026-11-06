# Plate N Platter: accounts, meal slots, prep tiers and health tags

This update is built on the `project/milestone-2` branch (the newest code). `main` still has the
Milestone 1 code, so merge `project/milestone-2` into `main` before you add this update.

## Why the features were missing

| Problem in the build | Cause found in the code |
| --- | --- |
| No sign-up or log-in screen, no way to change the name | `PlateNPlatterApp` opened the tabs straight away. `RegisterScreen` was only used by the old Milestone 1 `App`, and there was no log-in screen at all. The `users` table was only created by the old `App` (`Database.initialize()`), and `*.db` is in `.gitignore`, so on a fresh clone signing up would fail with "no such table: users". |
| No Breakfast / Lunch / Dinner | Every tab reads and writes the `meal_history` table, which only had `plan_date, recipe_name, category, prep_minutes`. "Category" is the recipe type (Breakfast, MainCourse, Dessert), not the meal of the day. |
| Streak badge does not sort by prep time | `StreakCalculator` only counted days. Nothing turned prep minutes into a tier. |
| No calorie or health classification | `meal_history` had no calories or healthy level column, and nothing classified a meal. |
| Meals not tied to a user | `meal_history` had no `user_id`, so every user would see every meal. |

## What changed

**Accounts**
- New `LoginScreen` is the first page. "Create an account" opens `RegisterScreen`; after signing up you go back to log in with the username filled in.
- New `service/AuthService` holds every rule (username 3 to 20 letters/digits/dots/underscores, age 1 to 120, password at least 6 characters, passwords match, username not taken, ignoring upper/lower case). Passwords are still salted and hashed with `PasswordHasher` (PBKDF2); the real password is never saved.
- New **Account** tab: change the display name (saved with `UserDao.updateName`) and log out. The header greets the user by that name ("Good afternoon, Jon!") and has a Log out button.
- Log-in, sign-up and rename run in background `Task`s, like the other tabs.

**Meal slots, calories and healthy level** (all saved in `meal_history`)
- New columns: `user_id`, `meal_slot` (BREAKFAST, LUNCH, DINNER, SNACK), `calories` (0 = not known) and `healthy_level` (HEALTHY, MODERATE, INDULGENT), each with a `CHECK` rule, plus a foreign key to `users`.
- An old database file is **upgraded automatically** the first time the app starts (`MealPlanHistoryDao.initSchema` adds the missing columns and fills in the old rows). Nothing is lost. Meals saved before accounts existed are given to the first user who logs in.
- Every query now only sees the logged-in user's meals.
- Recipes tab: pick the **Meal** (Breakfast, Lunch, Dinner, Snack), type the **Calories**, and pick a **Healthy level** or leave it on **Auto**. A line under the boxes says what Auto chose and why.
- Today tab: meals are grouped under Breakfast, Lunch, Dinner (and Snack when there is one). An empty meal has an "Add" button that opens the Recipes tab with that slot picked.
- Meal Plan calendar: a day is green, yellow or red by the healthy level most of its meals have (a tie shows the less healthy color).
- Every meal row shows its slot, a green/yellow/red health chip, its prep tier and its calories.
- CSV import still reads the old 4-column files, and also reads 3 optional columns: `meal_slot,calories,healthy_level`.

**Prep-time tiers and the streak badge**
- New `model/PrepTier`: **Quick** = 15 min or less, **Average** = 16 to 45 min, **Slow** = more than 45 min. It is never stored; it is always worked out from `prep_minutes`, so it can never go out of date.
- The header streak badge now shows the streak's meals by tier ("3 quick · 2 average · 1 slow"). The badge is named after the tier the user cooks most: **Quick Cook**, **Home Cook** or **Slow-Food Chef** (hover for the full breakdown). The Today tab shows the same badge.

**Analytics** (now fed by the new data, `service/StatisticsService`)
- **Week** view (with < and > to move weeks) or **All time** view.
- Grouped bar chart "Meals Planned per Day by Healthy Level, Week of ...", with **Group bars by**: Healthy level, Prep time, or Meal slot.
- Number cards: meals, healthy share, average prep time, average calories (only meals with calories count).
- Breakdown panel: by healthy level, by prep time, by meal slot.

## The healthy-level rule (for the defense)

Each slot has a share of a 2,000-calorie day, the reference day used on nutrition labels:
Breakfast 500, Lunch 650, Dinner 650, Snack 200 (500 + 650 + 650 + 200 = 2,000).

| Calories of the meal | Level |
| --- | --- |
| at or under the slot's budget | Healthy |
| up to 1.5 times the budget | Moderate |
| more than 1.5 times the budget | Indulgent |
| not given (0) | from the recipe type: Vegetarian, Vegan, Seafood = Healthy; Dessert = Indulgent; the rest Moderate |

Example: a 600 kcal lunch is Healthy (budget 650), but a 400 kcal snack is Indulgent (more than 1.5 × 200 = 300).
The user can always pick a different level by hand. Code: `util/HealthClassifier`, `model/MealSlot`.

## How to run and demo

```bash
mvn test          # runs every JUnit test
mvn javafx:run    # starts PlateNPlatterApp
```

Demo order that shows every fix:
1. Start the app: the Log in page appears. Click "Create an account", sign up, and log in.
2. The header says "Good ..., <your name>!". Open **Account**, change the name, press Save: the header changes at once.
3. **Today**: Breakfast, Lunch and Dinner each say "Nothing planned". Click "Add lunch".
4. **Recipes** opens with Lunch picked. Search "chicken", type 600 calories: the line under the boxes says "Auto: Healthy (600 kcal vs 650 kcal budget for Lunch)". Change it to 1100: it says Indulgent. Add it.
5. **Meal Plan**: today's day is colored by the meals' level.
6. The header streak badge now shows the tier line; hover it to see the badge name and the Quick / Average / Slow rule.
7. **Analytics**: switch "Group bars by" between Healthy level, Prep time and Meal slot; switch Week / All time. Add or remove a meal and press Refresh: the chart changes.
8. Log out, sign up a second user, log in: that user sees an empty plan (meals belong to their owner).

## Tests added

`PrepTierTest`, `MealSlotAndLevelTest`, `HealthClassifierTest`, `StatisticsServiceTest`, `StreakBadgeTest`,
`AuthServiceTest`, `UserMealsAndUpgradeTest` (per-user meals, foreign key, claiming old meals, upgrading an
old database file), plus new cases in `CsvRecipeParserTest`, `DatabaseConnectorTest` and `InMemoryPlanStoreTest`.
All the old tests are unchanged and still apply.

## Differences from the design document to update

- The design document calls the tiers Quick / Easy / Hard; the app now uses **Quick / Average / Slow** (same 15 and 45 minute limits).
- In the app, the slot, calories and healthy level are columns of `meal_history` (the table every tab uses), not of a separate `Recipe` / `MealPlanEntry` table.
- Class names: `LoginController` / `RegisterController` are `LoginScreen` / `RegisterScreen`; `StatisticsController` is `AnalyticsScreen` + `AnalyticsChart`; `StreakService` is `util/StreakCalculator`. `AuthService`, `PasswordHasher` and `StatisticsService.countByDayAndLevel` keep the document's names.
