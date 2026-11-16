package com.platenplatter.ui;

import com.platenplatter.controller.AnalyticsController;
import com.platenplatter.controller.AuthController;
import com.platenplatter.controller.ImportController;
import com.platenplatter.controller.MealPlanController;
import com.platenplatter.controller.RecipeController;
import com.platenplatter.controller.ShoppingListController;
import com.platenplatter.controller.TodayController;
import com.platenplatter.model.User;
import com.platenplatter.util.DataChangeNotifier;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

/**
 * The app after log-in: the navigation bar on top and one page below it.
 * Clicking a pill swaps the page with a short animation: it fades in while sliding up 16 px.
 * It also wires the Observer pattern: after a change, every page refreshes.
 */
public class AppShell {
    /** Window heights below this many pixels get the compact banner (see HeaderBar). */
    static final double COMPACT_BELOW = 760;
    /** How many background jobs can run at the same time (the CSV import uses one of them). */
    static final int WORKERS = 4;
    private static ExecutorService workers;

    public static final List<String> PAGES = List.of(
            "Today", "Meal Plan", "Recipes", "Shopping List", "Import", "Analytics", "Account");

    private final BorderPane root = new BorderPane();
    private final StackPane content = new StackPane();
    private final Map<String, Parent> pages = new LinkedHashMap<>();
    private final Map<String, Runnable> onShow = new HashMap<>();
    private final HeaderBar header;
    private final ImportScreen importScreen;
    private final User user;

    /**
     * The controllers of one logged-in user, made by PlateNPlatterApp (the one place where the
     * app is wired together): every page gets its own controller and never opens the database.
     */
    public record Controllers(TodayController today, RecipeController recipes,
            MealPlanController plan, ShoppingListController shopping, ImportController imports,
            AnalyticsController analytics, AuthController account) { }

    public AppShell(Controllers controllers, User user, Runnable onLogout,
            Runnable onAccountDeleted) {
        this.user = user;
        DataChangeNotifier notifier = new DataChangeNotifier();
        header = new HeaderBar(controllers.today(), PAGES, this::open, onLogout);
        header.setUserName(user.getDisplayName());

        RecipesScreen recipes = new RecipesScreen(controllers.recipes());
        TodayScreen today = new TodayScreen(controllers.today(), slot -> {
            recipes.setDate(LocalDate.now());
            recipes.setSlot(slot);
            open("Recipes");
        }, () -> open("Shopping List"));
        // Today's featured meal reuses Recipes' picks
        recipes.setOnPicksLoaded(today::showFeatured);
        today.setOnPlanFeatured(meal -> {
            recipes.setDate(LocalDate.now());
            open("Recipes");
            recipes.openMeal(meal);
        });
        MealPlanScreen plan = new MealPlanScreen(controllers.plan(), date -> {
            recipes.setDate(date);
            open("Recipes");
        });
        ShoppingListScreen shopping = new ShoppingListScreen(controllers.shopping());
        importScreen = new ImportScreen(controllers.imports());
        AnalyticsScreen analytics = new AnalyticsScreen(controllers.analytics());
        AccountScreen account = new AccountScreen(controllers.account(), user,
                renamed -> header.setUserName(renamed.getDisplayName()), onLogout);
        account.setOnDeleted(onAccountDeleted);

        addPages(today, plan, recipes, shopping, analytics, account);

        recipes.setOnPlanChanged(notifier::fire);         // who rings the bell
        plan.setOnPlanChanged(notifier::fire);
        importScreen.setOnImportFinished(notifier::fire);
        header.setOnNewDay(notifier::fire);
        List.<Runnable>of(header::refresh, today::refresh, plan::refresh, shopping::refresh,
                analytics::refresh)
                .forEach(notifier::addListener);           // who listens

        root.setTop(header.getView());
        root.setCenter(content);
        Theme.apply(root);
        // a short window (small screen or Windows scaling) gets the compact banner
        root.heightProperty().addListener((obs, old, now) ->
                header.setCompact(now.doubleValue() < COMPACT_BELOW));
        header.refresh();
        open("Today");
    }

    /** Puts every page in the content map and says what refreshes when a page is opened. */
    private void addPages(TodayScreen today, MealPlanScreen plan, RecipesScreen recipes,
            ShoppingListScreen shopping, AnalyticsScreen analytics, AccountScreen account) {
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

    /**
     * Stops the clock and any running import, and forgets the photos (log out or window closed).
     */
    public void stop() {
        header.stop();
        importScreen.cancelImport();
        MealPhotos.clear();
    }

    private void show(Node page) {
        content.getChildren().setAll(page);               // swap the page in
        for (Node node : page.lookupAll(".scroll-pane")) { // a page always opens at its top
            if (node instanceof ScrollPane scroller) {
                scroller.setVvalue(0);
            }
        }
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

    /**
     * The app's ONE pool of background threads, named "pnp-worker-1" to "pnp-worker-4". It is made
     * by PlateNPlatterApp (the composition root) and shut down when the window closes. The threads
     * are daemons, so they can never keep the app open by themselves.
     */
    public static ExecutorService newWorkers() {
        AtomicInteger number = new AtomicInteger(1);
        ThreadFactory named = job -> {
            Thread thread = new Thread(job, "pnp-worker-" + number.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
        return Executors.newFixedThreadPool(WORKERS, named);
    }

    /** Tells every page which pool to use (called once by PlateNPlatterApp.start). */
    public static synchronized void useWorkers(ExecutorService pool) {
        workers = pool;
    }

    /**
     * Runs a Task on the shared pool, so the window never freezes. Its result comes back on the
     * JavaFX thread through the Task's setOnSucceeded / setOnFailed.
     */
    public static synchronized void runInBackground(Task<?> task) {
        if (workers == null || workers.isShutdown()) {
            workers = newWorkers(); // only when no app made one (for example a test)
        }
        workers.execute(task);
    }
}
