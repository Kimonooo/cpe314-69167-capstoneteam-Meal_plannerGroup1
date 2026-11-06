package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.util.DataChangeNotifier;
import com.platenplatter.util.PlanStore;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;

import java.time.LocalDate;

/**
 * Puts the whole application together for ONE logged-in user: the live header on top and seven
 * tabs below it (Today, Meal Plan, Recipes, Shopping List, Import, Analytics, Account).
 * It only knows the PlanStore interface, so it works with the real database (MealPlanHistoryDao,
 * which only sees this user's meals) or with InMemoryPlanStore for previews and tests.
 *
 * How the tabs talk to each other: whenever something changes (a meal is added or removed, or an
 * import finishes) the screen rings a DataChangeNotifier bell, and every other screen refreshes itself.
 */
public final class AppShell {
    private AppShell() { }

    /** Everything the caller needs: the root to show, the tab pane, the header and the user. */
    public static final class Shell {
        private final BorderPane root;
        private final TabPane tabs;
        private final HeaderBar header;
        private final ImportScreen importScreen;
        private final User user;

        Shell(BorderPane root, TabPane tabs, HeaderBar header, ImportScreen importScreen, User user) {
            this.root = root;
            this.tabs = tabs;
            this.header = header;
            this.importScreen = importScreen;
            this.user = user;
        }

        public BorderPane root() { return root; }
        public TabPane tabs() { return tabs; }
        public HeaderBar header() { return header; }
        public ImportScreen importScreen() { return importScreen; }
        public User user() { return user; }

        /** Call when the window closes or the user logs out: stops the clock and any running import. */
        public void stop() {
            header.stop();
            importScreen.cancelImport();
        }
    }

    /** For the previews and the old App: a "Guest" user, nothing to log out from. */
    public static Shell build(PlanStore store, MealDbClient client) {
        User guest = new User(0, "Guest", "guest", 0, "", "", "");
        return build(store, client, guest, null, null);
    }

    /**
     * @param auth     saves a new display name (null = the name only changes for this session)
     * @param onLogout what Log out does (null = no Log out button)
     */
    public static Shell build(PlanStore store, MealDbClient client, User user, AuthService auth, Runnable onLogout) {
        DataChangeNotifier notifier = new DataChangeNotifier();
        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("pnp-tabs");
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        HeaderBar header = new HeaderBar(store);
        header.setUserName(user.getName());
        RecipesScreen recipes = new RecipesScreen(client, store);
        ImportScreen imports = new ImportScreen(store::addImported);
        AnalyticsScreen analytics = new AnalyticsScreen(store);
        ShoppingListScreen shopping = new ShoppingListScreen(store, client);

        Tab todayTab = new Tab("Today");
        Tab planTab = new Tab("Meal Plan");
        Tab recipesTab = new Tab("Recipes", recipes.getView());
        Tab shoppingTab = new Tab("Shopping List", shopping.getView());
        Tab importTab = new Tab("Import", imports.getView());
        Tab analyticsTab = new Tab("Analytics", analytics.getView());
        Tab accountTab = new Tab("Account");

        // "Add" on an empty Breakfast / Lunch / Dinner of today: open Recipes on today, with that slot picked
        TodayScreen today = new TodayScreen(store,
                slot -> {
                    recipes.setDate(LocalDate.now());
                    recipes.setSlot(slot);
                    tabs.getSelectionModel().select(recipesTab);
                },
                () -> tabs.getSelectionModel().select(shoppingTab));
        MealPlanScreen plan = new MealPlanScreen(store, date -> {
            recipes.setDate(date);
            tabs.getSelectionModel().select(recipesTab);
        });
        AccountScreen account = new AccountScreen(auth, user, renamed -> header.setUserName(renamed.getName()), onLogout);
        todayTab.setContent(today.getView());
        planTab.setContent(plan.getView());
        accountTab.setContent(account.getView());
        tabs.getTabs().addAll(todayTab, planTab, recipesTab, shoppingTab, importTab, analyticsTab, accountTab);
        header.setOnLogout(onLogout);

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
        return new Shell(root, tabs, header, imports, user);
    }
}
