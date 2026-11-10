# App.java wiring (Kimberly, Day 2) - do this only after the PRs from Nesley, Yvonne and Ken are merged
Where your App.java creates the existing tabs (Sign up, Calendar), add the three new ones.
Names below are the ones in the zips; replace `conn` with however UserDao/Database gets its Connection.

    Connection conn = ...;   // same way the existing code gets the SQLite connection
    MealPlanHistoryDao historyDao = new MealPlanHistoryDao(conn);
    historyDao.initSchema();
    MealDbClient mealDb = new MealDbClient();

    Tab recipesTab   = new Tab("Recipes",   new RecipesScreen(mealDb, historyDao::insert).getView());
    Tab importTab    = new Tab("Import",    new ImportScreen(historyDao::insert).getView());
    Tab analyticsTab = new Tab("Analytics",
            new AnalyticsScreen(historyDao::getPlannedCategories, historyDao::getPrepMinutes).getView());
    // add them to the TabPane next to the existing tabs, and make them not closable:
    //   tabPane.getTabs().addAll(recipesTab, importTab, analyticsTab);

Imports to add: com.platenplatter.api.MealDbClient, com.platenplatter.db.MealPlanHistoryDao,
com.platenplatter.ui.* (same package as App? then none), java.sql.Connection.
Optional (nice for the demo): make the Calendar's "Meals on <date>" also read historyDao.findByDate(date).
Test: add a meal in Recipes -> import a CSV -> Analytics Refresh shows it.
