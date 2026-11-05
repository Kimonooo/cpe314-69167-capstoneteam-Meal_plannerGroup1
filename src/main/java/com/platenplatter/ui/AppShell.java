package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.DataChangeNotifier;
import com.platenplatter.util.PlanStore;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

/**
 * Puts the whole application together: the live header on top and six tabs below it.
 * It only knows the PlanStore interface, so it works with the real database (MealPlanHistoryDao)
 * or with InMemoryPlanStore for previews and tests.
 *
 * How the tabs talk to each other: whenever something changes (a meal is added or removed, or an
 * import finishes) the screen rings a DataChangeNotifier bell, and every other screen refreshes itself.
 */
public final class AppShell {
    private AppShell() { }

    /** Everything the caller needs: the root to show, the tab pane, and the header (to stop its clock). */
    public static final class Shell {
        private final BorderPane root;
        private final TabPane tabs;
        private final HeaderBar header;
        private final ImportScreen importScreen;

        Shell(BorderPane root, TabPane tabs, HeaderBar header, ImportScreen importScreen) {
            this.root = root;
            this.tabs = tabs;
            this.header = header;
            this.importScreen = importScreen;
        }

        public BorderPane root() { return root; }
        public TabPane tabs() { return tabs; }
        public HeaderBar header() { return header; }
        public ImportScreen importScreen() { return importScreen; }

        /** Call when the window closes. */
        public void stop() {
            header.stop();
        }
    }

    public static Shell build(PlanStore store, MealDbClient client) {
        DataChangeNotifier notifier = new DataChangeNotifier();
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("pnp-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        HeaderBar header = new HeaderBar(store);
        RecipesScreen recipes = new RecipesScreen(client, store);
        ImportScreen imports = new ImportScreen(store::addImported);
        AnalyticsScreen analytics = new AnalyticsScreen(store::categories, store::prepMinutes);
        ShoppingListScreen shopping = new ShoppingListScreen(store, client);

        Tab todayTab = new Tab("Today");
        Tab planTab = new Tab("Meal Plan");
        Tab recipesTab = new Tab("Recipes", recipes.getView());
        Tab shoppingTab = new Tab("Shopping List", shopping.getView());
        Tab importTab = new Tab("Import", imports.getView());
        Tab analyticsTab = new Tab("Analytics", analytics.getView());

        TodayScreen today = new TodayScreen(store,
                () -> tabs.getSelectionModel().select(recipesTab),
                () -> tabs.getSelectionModel().select(shoppingTab));
        MealPlanScreen plan = new MealPlanScreen(store, date -> {
            recipes.setDate(date);
            tabs.getSelectionModel().select(recipesTab);
        });
        todayTab.setContent(today.getView());
        planTab.setContent(plan.getView());
        tabs.getTabs().addAll(todayTab, planTab, recipesTab, shoppingTab, importTab, analyticsTab);

        // who rings the bell
        recipes.setOnPlanChanged(notifier::fire);
        imports.setOnImportFinished(notifier::fire);
        plan.setOnPlanChanged(notifier::fire);
        header.setOnNewDay(notifier::fire);
        // who listens
        notifier.addListener(header::refresh);
        notifier.addListener(today::refresh);
        notifier.addListener(plan::refresh);
        notifier.addListener(shopping::refresh);
        notifier.addListener(analytics::refresh);
        notifier.addListener(recipes::refresh);
        // also refresh a tab when you open it
        todayTab.setOnSelectionChanged(e -> { if (todayTab.isSelected()) { today.refresh(); } });
        planTab.setOnSelectionChanged(e -> { if (planTab.isSelected()) { plan.refresh(); } });
        shoppingTab.setOnSelectionChanged(e -> { if (shoppingTab.isSelected()) { shopping.refresh(); } });
        analyticsTab.setOnSelectionChanged(e -> { if (analyticsTab.isSelected()) { analytics.refresh(); } });

        BorderPane root = new BorderPane();
        root.setTop(header.getView());
        root.setCenter(tabs);
        Theme.apply(root);
        return new Shell(root, tabs, header, imports);
    }
}
