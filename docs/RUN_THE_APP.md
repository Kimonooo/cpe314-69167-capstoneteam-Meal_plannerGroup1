# Run Plate N Platter (Kimberly's laptop)

## Run it
In Command Prompt, in the project folder:

    mvn clean test          (about 100 tests, must say BUILD SUCCESS)
    mvn javafx:run          (opens the finished app)

The first run may download things. If you installed the ALL-IN-ONE zip, `pom.xml` already starts `com.platenplatter.PlateNPlatterApp`.

## What you should see
A maroon header with today's date, a live clock (the seconds count up by themselves) and a gold streak badge. Six tabs:

| Tab | What it does |
|---|---|
| Today | streak, best streak, meals today, the 7 days of this week, today's meals |
| Meal Plan | month calendar (gold = meals planned, ring = today). Click a day to see, remove or add meals |
| Recipes | search TheMealDB, read a recipe, add it to the plan (its ingredients are saved) |
| Shopping List | one list for the days you pick, grouped by aisle, tick items, copy or save |
| Import | load a big CSV on a background thread, with a progress bar and Cancel |
| Analytics | three number cards, a bar chart and a breakdown, counted from the database |

Pictures of every tab are in `docs/preview_screens/` (they are only a preview: take your own screenshots for the Design Document).

## Practice mode (saves nothing)
    mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.Milestone2Preview

## The data
The app keeps everything in the file `platenplatter.db` in the project folder. To start fresh, close the app and delete that file. It is created again on the next run.
Sample CSV files for the Import tab are in `sample_data/`: `meal_history_small.csv` (5 rows), `meal_history_3000.csv`, `meal_history_30000.csv` (the big one, about 4 seconds) and `meal_history_bad_rows.csv` (5 bad rows).

## Want the old Sign up and Calendar tabs too?
Those two tabs are in the Milestone 1 app (`com.platenplatter.App`). To show the new tabs next to them, add ONE line to App.java right after its own tabs are added to the TabPane:

    tabPane.getTabs().addAll(com.platenplatter.ui.Milestone2Tabs.createWithDefaultDatabase());

(use the real name of your TabPane variable). To run that old app: `mvn javafx:run -Dapp.mainClass=com.platenplatter.App`.

## If something goes wrong
| Problem | What to do |
|---|---|
| `BUILD FAILURE` | Copy the first line with ERROR and send it to the group chat |
| A red "could not start" window | The database file may be open in another program. Close it and run again |
| Recipes say "Could not reach TheMealDB" | Check the internet |
| The window is too small | Drag its corner, or maximise it |
