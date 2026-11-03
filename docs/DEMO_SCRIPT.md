# Milestone 2 demo script (5-7 min)
1. (2 min) Run app: Sign up -> Recipes: search "chicken" -> Add to plan -> Calendar shows the meal.
2. (1 min) Import tab: choose sample_data/meal_history_3000.csv -> Import. While the ProgressBar moves, click another tab to show the UI is responsive.
3. (1 min) Show API succeeding: turn Wi-Fi off, search -> error message (not a crash); Wi-Fi on, search -> results. Open MealDbClient.parseMeals to show the JSON becomes MealData objects.
4. (1 min) Analytics tab: show the chart. Explain: DAO returns categories -> MealPlanAnalyzer.countByCategory (Map) -> BarChart. Then import meal_history_small.csv / add one dessert, press Refresh: the bar changes.
5. (1 min) Run `mvn test` live: all green.
6. (1 min) Be ready: each member can explain one line of their own commit (git log --author="you").
Also ready: docs/KNOWN_ISSUES.md and docs/REQUIREMENT_TO_CODE_MAP.md.
