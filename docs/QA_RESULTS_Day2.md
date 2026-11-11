# QA Results - Day 2 (Venice)

| # | Test | Steps (read these out loud) | You should see | Pass / Fail | Notes |
|---|---|---|---|---|---|
| 1 | The window | Look at the top of the app. | A maroon header with today's date, a clock that changes every second, and a gold streak badge. Six tabs: Today, Meal Plan, Recipes, Shopping List, Import, Analytics. | | |
| 2 | Clock is live | Watch the clock for 5 seconds. | The seconds count up by themselves. | | |
| 3 | Date is right | Compare with the date on the laptop. | Same date. | | |
| 4 | Today tab | Click Today. | Streak, best streak, meals today, the 7 days of this week (today has a maroon ring). | | |
| 5 | Search works | Recipes tab. Click the **chicken** button. | A spinner, then a list. The window keeps moving. | | |
| 6 | Click a recipe | Click any recipe. | Name, colored chips, ingredient chips and cooking steps. | | |
| 7 | Nothing found | Search `zzzzqq`. | Red message "No recipes found". No crash. | | |
| 8 | Empty box | Clear the box, press Search. | Red message "Please type something to search." | | |
| 9 | Offline | Turn off the Wi-Fi. Search `beef`. Turn Wi-Fi on. | Red "Could not reach TheMealDB...". No crash. | | |
| 10 | Add to plan | Search `chicken`, click a recipe, press **Add to my plan**. | Green "Added..." and the meal under "Planned for ...". | | |
| 11 | Streak updates | Look at the header after step 10 (if today had no meal before). | The streak badge changed or stayed alive. | | |
| 12 | Meal Plan calendar | Click Meal Plan. Click today, then another gold day. | Gold days have meals. The clicked day is dark. Its meals show on the right. | | |
| 13 | Remove a meal | Click **Remove** next to a meal. | The meal disappears and the day may turn gray. | | |
| 14 | Add from the calendar | Click a day, press **Add a meal for this day**. | You land on Recipes with that date chosen. | | |
| 15 | Shopping list | Shopping List tab, **Next 7 days**. | Items grouped by aisle, each aisle A to Z, "for N meals" notes. | | |
| 16 | Ticking | Tick 3 items. | They are crossed out and the bar says "3 of N bought". | | |
| 17 | Copy list | Press **Copy list**, paste into Notepad. | A text list with [x] for ticked items. | | |
| 18 | Missing ingredients | Add a meal that is not a built-in dish by importing it, or look for the yellow box. Press **Look up online**. | A message says how many were found. | | |
| 19 | Big import stays responsive | Import tab. Choose `sample_data/meal_history_30000.csv`. Press Import. While the bar moves, click other tabs. | The bar moves. The window does NOT freeze. | | |
| 20 | After the import | When it ends. | "Done!", three number cards (30,000 imported), the streak badge updated by itself. | | |
| 21 | Bad rows | Import `sample_data/meal_history_bad_rows.csv`. | 2 imported, 5 skipped. No crash. | | |
| 22 | Cancel | Start the 30000 import again, press **Cancel** after one second. | "Import cancelled." Some rows saved, not all. | | |
| 23 | Chart is real | Analytics tab, then the CHANGED INPUT test (`docs/ChangedInput_Test.md`). | The category goes up by exactly the number of meals added. | | |
| 24 | Chart labels | Look at the chart. | Title, x-axis label, y-axis label, legend. | | |
| 25 | `mvn clean test` | Run it in the terminal. | BUILD SUCCESS, Failures: 0 (about 100 tests). | | |

Count: ____ passed out of 25.
Tip: take a screenshot of anything strange (Windows key + Shift + S).
