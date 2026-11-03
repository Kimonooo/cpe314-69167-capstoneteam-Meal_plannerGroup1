# Known Issues / Limitations (Venice keeps this updated; show on request at the demo)
| # | Issue / limitation | Where | Workaround or plan |
|---|---|---|---|
| 1 | TheMealDB search needs internet; offline shows an error message | MealDbClient / RecipesScreen | Message tells the user to check the connection |
| 2 | TheMealDB has no prep time, so the user sets it with a spinner (default 30 min) | RecipesScreen | Documented limitation |
| 3 | Import reads the whole CSV into memory (fine for thousands of rows, not millions) | CsvImportTask | Could stream line by line later |
| 4 | Bad rows (bad date, non-numeric or negative minutes, wrong field count) are skipped and counted | CsvRecipeParser | Skipped count is shown in the status label |
| 5 | SQLite uses PRAGMA synchronous=OFF to speed up bulk import (small risk if the PC crashes mid-import) | MealPlanHistoryDao | Acceptable for course data |
| 6 | [FILL: real bugs found during QA] | | |
