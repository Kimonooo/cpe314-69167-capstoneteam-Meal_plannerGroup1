package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.service.RecommendationService;
import com.platenplatter.util.DataChangeNotifier;
import com.platenplatter.util.PlanStore;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * The app after log-in: the navigation bar on top and one page below it.
 * Clicking a pill swaps the page with a short animation: it fades in while sliding up 16 px.
 * It also wires the Observer pattern: after a change, every page refreshes.
 */
public class AppShell {
    public static final List<String> PAGES = List.of(
            "Today", "Meal Plan", "Recipes", "Shopping List", "Import", "Analytics", "Account");

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Map<String, Parent> pages = new LinkedHashMap<>();
    private final Map<String, Runnable> onShow = new HashMap<>();
    private final HeaderBar header;
    private final ImportScreen importScreen;
    private final User user;

    public AppShell(PlanStore store, MealDbClient client, User user, AuthService auth, Runnable onLogout) {
        this.user = user;
        DataChangeNotifier notifier = new DataChangeNotifier();
        header = new HeaderBar(store, PAGES, this::open, onLogout);
        header.setUserName(user.getDisplayName());

        RecipesScreen recipes = new RecipesScreen(client, new RecommendationService(client), store);
        TodayScreen today = new TodayScreen(store, slot -> {
            recipes.setDate(LocalDate.now());
            recipes.setSlot(slot);
            open("Recipes");
        }, () -> open("Shopping List"));
        MealPlanScreen plan = new MealPlanScreen(store, date -> {
            recipes.setDate(date);
            open("Recipes");
        });
        ShoppingListScreen shopping = new ShoppingListScreen(store, client);
        importScreen = new ImportScreen(store);
        AnalyticsScreen analytics = new AnalyticsScreen(store);
        AccountScreen account = new AccountScreen(auth, user,
                renamed -> header.setUserName(renamed.getDisplayName()), onLogout);

        pages.put("Today", today.getView());
        pages.put("Meal Plan", plan.getView());
        pages.put("Recipes", recipes.getView());
        pages.put("Shopping List", shopping.getView());
        pages.put("Import", importScreen.getView());
        pages.put("Analytics", analytics.getView());
        pages.put("Account", account.getView());
        onShow.put("Today", today::refresh);              // a page also refreshes when it is opened
        onShow.put("Meal Plan", plan::refresh);
        onShow.put("Recipes", recipes::refreshPicks);
        onShow.put("Shopping List", shopping::refresh);
        onShow.put("Analytics", analytics::refresh);

        recipes.setOnPlanChanged(notifier::fire);         // who rings the bell
        plan.setOnPlanChanged(notifier::fire);
        importScreen.setOnImportFinished(notifier::fire);
        header.setOnNewDay(notifier::fire);
        List.<Runnable>of(header::refresh, today::refresh, plan::refresh, shopping::refresh, analytics::refresh)
                .forEach(notifier::addListener);           // who listens

        root.setTop(header.getView());
        root.setCenter(content);
        Theme.apply(root);
        header.refresh();
        open("Today");
    }

    public Parent getView() {
        return root;
    }

    public User getUser() {
        return user;
    }

    /** Opens a page by its name, the same as clicking its pill. */
    public void open(String name) {
        header.select(name);
        show(pages.get(name));
        onShow.getOrDefault(name, () -> { }).run();
    }

    /** Stops the clock and any running import (on log out or when the window closes). */
    public void stop() {
        header.stop();
        importScreen.cancelImport();
    }

    private void show(Node page) {
        content.getChildren().setAll(page);               // swap the page in
        Duration time = Duration.millis(250);
        FadeTransition fade = new FadeTransition(time, page);
        fade.setFromValue(0);                             // invisible ...
        fade.setToValue(1);                               // ... to fully shown
        TranslateTransition slide = new TranslateTransition(time, page);
        slide.setFromY(16);                               // 16 px lower ...
        slide.setToY(0);                                  // ... to its place
        slide.setInterpolator(Interpolator.EASE_OUT);     // fast start, soft stop
        new ParallelTransition(fade, slide).play();
    }

    /** Runs a Task on its own daemon thread, so the window never freezes and never stays open by itself. */
    public static void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
