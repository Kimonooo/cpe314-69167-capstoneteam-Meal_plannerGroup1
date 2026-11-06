# QA Test Plan (Venice) - run on Day 2 and again on Day 3
| # | Test | Steps | Expected | Result (Pass/Fail) | Notes |
|---|---|---|---|---|---|
| 1 | Search works | Recipes tab > "chicken" > Search | List of recipes appears, window does not freeze | | |
| 2 | Search - nothing found | Search "zzzzqq" | Message "No recipes found" | | |
| 3 | Search - empty box | Press Search with empty box | Message "Please type something" | | |
| 4 | Search - offline | Turn Wi-Fi off, Search | Friendly error, no crash | | |
| 5 | Add to plan | Select a recipe > Add to plan | "Added ..." message; row saved | | |
| 6 | Add without selection | Press Add to plan with nothing selected | "Select a recipe first." | | |
| 7 | Big import stays responsive | Import meal_history_3000.csv; click other tabs while it runs | ProgressBar moves, UI not frozen | | |
| 8 | Bad rows | Import meal_history_bad_rows.csv | "Imported 2 rows, skipped 5" | | |
| 9 | Cancel | Start the 3000-row import, press Cancel | "Import cancelled." | | |
| 10 | Chart reflects data | Import small CSV, Refresh Analytics, note bars | Bars = Breakfast 1, MainCourse 2, Dessert 2 | | |
| 11 | CHANGED INPUT | Add 3 more Breakfast meals, Refresh | Breakfast bar grows; most-planned changes | | |
| 12 | Empty database | New empty DB > Analytics | "No meal data yet" message, no crash | | |
| 13 | mvn test | Run in terminal | 0 failures, 6+ tests | | |
| 14 | Existing features still work | Sign up, Calendar, Cooking queue, Undo | Same as Milestone 1 | | |
