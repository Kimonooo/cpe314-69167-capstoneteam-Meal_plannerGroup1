# Section 6 - Concurrency Design   (author: Yvonne)
How to use this file: read it, then replace every [YOUR TURN] with what YOU saw on screen. Send the finished text to Kirk.
Formatting reminders: Calibri 11. Table captions ABOVE tables. Figure captions BELOW pictures. Numbers continue from Week 9.

## 6. Concurrency Design

The JavaFX window is drawn by one thread, the **JavaFX Application Thread**. If that thread is busy with slow work, the window freezes. So every slow job in Plate N Platter runs on a **background thread**, and only small updates come back to the window.

### 6.1 Long-running operations

Table N: Operations that run on a background thread

| Operation | Class | Started by | What the user sees meanwhile |
|---|---|---|---|
| Bulk CSV import (thousands of rows) | `CsvImportTask` | **Import** button in `ImportScreen` | Moving progress bar, percent, live count of imported and skipped rows, working Cancel button |
| Recipe search on TheMealDB (network) | `Task` in `RecipesScreen` | **Search** button | Spinning progress indicator, "Searching..." message |
| Saving a meal to the database | `Task` in `RecipesScreen` | **Add to my plan** button | Button disabled until the save is done |
| Reading the data for the chart | `Task` in `AnalyticsScreen` | **Refresh** button, or opening the tab | "Loading..." message |
| Building the shopping list | `Task` in `ShoppingListScreen` | Choosing the days | The list appears when ready |
| "Look up online" for missing ingredients | `Task` in `ShoppingListScreen` | **Look up online** button | "Looking up ..." message, button disabled |
| Reading the calendar and the streak | `Task`s in `MealPlanScreen`, `TodayScreen`, `HeaderBar` | Opening a tab, or any change to the plan | The old numbers stay on screen until the new ones arrive |

### 6.2 Threading approach

- Each job is a `javafx.concurrent.Task`, started with `new Thread(task)` and `setDaemon(true)`, so a closing app is never held up by a worker.
- `CsvImportTask.call()` reads the file, sends every good row to the database through a `RowSink`, and counts bad rows. It reports progress with `updateProgress()` and `updateMessage()`, but only every 50 rows, so the UI thread is not flooded with updates.
- The Cancel button calls `task.cancel()`; the loop checks `isCancelled()` and stops. A bad row never stops the import (it is skipped and counted).
- The result handlers `setOnSucceeded`, `setOnFailed` and `setOnCancelled` run back on the JavaFX thread, so they can safely change labels and charts.
- The database object `MealPlanHistoryDao` has `synchronized` methods, and `DatabaseConnector` sets a 5-second SQLite busy timeout, so the import thread and the screen can use the database at the same time without errors.

### 6.2b The live clock (not a background thread)
The date and clock in the header are updated every second by a JavaFX `Timeline` (`KeyFrame` every 1 second, `INDEFINITE`). A `Timeline` runs on the JavaFX thread itself and does only a tiny job (formatting two short texts), so it never blocks the window. When the clock passes midnight, the header re-counts the streak and tells the other tabs (through `DataChangeNotifier`) to show the new day.

### 6.3 How the UI stays responsive

The progress bar is bound to `task.progressProperty()` and the status line to `task.messageProperty()`. JavaFX updates them by itself, so there is no extra code that touches the window from the worker thread.

[YOUR TURN: write what you did while the import was running. Example: "While importing meal_history_30000.csv (about 1.1 MB), we clicked the Recipes and Analytics tabs and the window kept responding. Import took about ___ seconds."]

Figure N: Import tab with the progress bar part-way    <- screenshot, caption goes BELOW
Figure N: Import tab after "Done" with the three number cards    <- screenshot, caption goes BELOW

### 6.4 Tests and edge cases
`CsvRecipeParserTest` and `SampleDataPipelineTest` check that a wrong field count, a bad date, a letter instead of a number and a negative number are all rejected without stopping the rest. `EndToEndPipelineTest` checks that the bad-rows file puts exactly 2 rows in the database and skips 5.
