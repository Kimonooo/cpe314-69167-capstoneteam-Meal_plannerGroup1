# Section 7 - Data Mining & Visualization Design   (author: Ken)
How to use this file: read it, do the CHANGED INPUT test (docs/ChangedInput_Test.md), then replace every [YOUR TURN]. Send the finished text to Kirk.
Formatting reminders: Calibri 11. Table captions ABOVE tables. Figure captions BELOW pictures. Numbers continue from Week 9.

## 7. Data Mining & Visualization Design

### 7.1 The mining step (frequency analysis)

The app mines its own meal plan. `PlanStore.categories()` (implemented by `MealPlanHistoryDao.getPlannedCategories()`, which runs `SELECT category FROM meal_history` as a PreparedStatement) gives back the category of every planned meal. `MealPlanAnalyzer` then analyses that list:

- `countByCategory()` counts how many meals fall in each category. It stores the answer in a `Map<String, Integer>` (a `TreeMap`, so the categories always come out in alphabetical order).
- `mostFrequent()` finds the category with the highest count.
- `averagePrepMinutes()` finds the average preparation time, ignoring missing or negative values.

The input is read from the database every time, so **changing the data changes the result and the chart**. Nothing is hard-coded.

Table N: Mining result for the 3,000-row sample file (meal_history_3000.csv)

| Category | Number of meals | Share |
|---|---|---|
| Breakfast | 747 | 24.9% |
| Dessert | 725 | 24.2% |
| MainCourse | 1,528 | 50.9% |
| **Total** | **3,000** | most planned = MainCourse, average prep time = 33.7 min |

### 7.2 The chart

`AnalyticsChart.build()` draws a JavaFX `BarChart` with:

- title: "Meals Planned per Category";
- x-axis label: "Recipe category"; y-axis label: "Number of meals planned";
- one series named "Meals planned", shown in a legend;
- a tidy side scale chosen by `ChartMath` (0, 500, 1000... for big numbers and 0, 1, 2... for small ones), so the labels never show half a meal.

**Why a bar chart:** we compare counts across separate categories that have no order (Breakfast, MainCourse, Dessert). Bars make the sizes easy to compare. A line chart would wrongly suggest a trend from one category to the next.

The Analytics tab also shows three number cards (total meals, most planned category, average prep time) and a breakdown list with each category's percentage.

### 7.3 Proof that it is not hard-coded (the "changed input" test)

[YOUR TURN: fill the table from docs/ChangedInput_Test.md]

Table N: Changed-input test

| Step | Breakfast | Dessert | MainCourse | Total |
|---|---|---|---|---|
| Start (before) | [YOUR TURN] | [YOUR TURN] | [YOUR TURN] | [YOUR TURN] |
| After adding 3 meals of ONE category | [YOUR TURN] | [YOUR TURN] | [YOUR TURN] | [YOUR TURN: exactly 3 more] |

The chart and the number cards changed together with the data, so the result is not hard-coded.

Figure N: Analytics chart BEFORE the change    <- screenshot, caption goes BELOW
Figure N: Analytics chart AFTER the change    <- screenshot, caption goes BELOW

### 7.4 Tests
`MealPlanAnalyzerTest` (counts, empty list, blank category, changed input, average), `ChartMathTest` (scale numbers) and `EndToEndPipelineTest` (CSV file to database to counts).

### 7.5 A second small analysis: the streak
`StreakCalculator` also analyses the plan, but by DATE instead of by category. It takes the set of days that have at least one planned meal (`SELECT DISTINCT plan_date`) and counts how many days in a row end today (the **current streak**), and the longest run up to today (the **best streak**). If today has no meal yet but yesterday does, the streak is still alive ("at risk"). The header shows the streak, and `StreakCalculatorTest` has 8 tests, including a gap, a missed day and planning ahead.
