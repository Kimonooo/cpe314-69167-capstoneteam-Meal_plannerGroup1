# Milestone 2 demo script (5 to 7 minutes) - run on Kimberly's laptop
Practise it TWICE on Day 3 with a phone timer. Every member must be able to explain their own part.
Before you start: the file `platenplatter.db` already has some meals, and Wi-Fi works.

| Time | Who | What to do | What to say |
|---|---|---|---|
| 0.5 min | Kimberly | Open the app (`mvn javafx:run`). Show the Today tab: the live date and clock, the streak, today's meals. | "The date and clock update every second. The streak counts days in a row with a planned meal." |
| 1 min | Nesley | Recipes tab: click **chicken**, click a recipe, press **Add to my plan**. Open the Meal Plan tab: today is gold. | "The search runs in a background Task. The recipe's ingredients are saved for the shopping list." |
| 1 min | Kirk | Shopping List tab: **Next 7 days**. Show the aisles, tick two items, press **Copy list**. | "Same ingredients from different meals are merged and counted, and grouped by aisle." |
| 1.5 min | Yvonne | Import tab: choose `meal_history_30000.csv`, press Import. WHILE the bar moves, click another tab and come back. | "The import runs on a background thread, so the window never freezes." |
| 1 min | Ken | Analytics tab: explain the cards and the chart. Add a meal, come back: the numbers changed. | "Counted from the database with a Map. If the data changes, the chart changes." |
| 1 min | Venice | Run `mvn clean test` live. Open one edge-case test and explain it. | "An edge case is an unusual input, like a bad CSV line." |
| 0.5 min | Anyone | The teacher asks one person about a line of their own code. | Stay calm. Say what you know. |

If something fails: say what you expected, show the error message the app gives, and move on. An honest "we are behind on X" earns more than silence.
