package com.platenplatter.ui;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.StreakCalculator;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The top of every page, like the header of a website:
 *   the navigation bar: the logo, one rounded pill per page, the user's initial and Log out;
 *   the banner under it: the greeting with the date and time, the title and subtitle of the open
 *   page, waves with a chef hat, fork and spoon along the bottom, and two pills on the right (the streak with its flame, the badge).
 * The clock ticks every second; at midnight it refreshes the streak and tells the other pages.
 */
public class HeaderBar {
    private record StreakData(Set<LocalDate> days, List<PlannedMeal> streakMeals) { }

    /** The title and subtitle the banner shows for a page (the page itself no longer repeats them). */
    private static final Map<String, String[]> TITLES = Map.of(
            "Today", new String[] {"Today", "Your streak, an idea for your next meal, and what is planned for today."},
            "Meal Plan", new String[] {"Meal Plan",
                    "Pick a day to see what is planned. Days are colored by how healthy their meals are."},
            "Recipes", new String[] {"Recipes",
                    "Search TheMealDB by name. Nothing is listed until you search, except today's picks."},
            "Shopping List", new String[] {"Shopping List",
                    "What to buy for one recipe you planned, grouped by food group."},
            "Import", new String[] {"Import Meal History",
                    "Load thousands of past meals from a CSV file. It runs in the background, so you can keep using the app."},
            "Analytics", new String[] {"Meal Plan Analytics",
                    "How healthy, how quick, and which meals? Counted straight from your meal plan."},
            "Account", new String[] {"My Account",
                    "Change the name the app calls you, log out, or delete your account."});

    private final PlanStore store;
    private final Supplier<LocalDateTime> now;
    private final VBox root = new VBox();
    private final ToggleGroup pills = new ToggleGroup();
    private final Label avatar = new Label();
    private final Label greetingLabel = new Label();
    private final Label pageTitle = new Label();
    private final Label pageSubtitle = new Label();
    private final ImageView flame = Theme.icon("streak", 18);
    private final ScaleTransition flamePulse = new ScaleTransition(Duration.millis(900), flame);
    private final Label streakChip = new Label("0-day streak");
    private final Label badgeChip = new Label("No badge yet");
    private final Label hintLabel = new Label("Plan a meal today to keep your streak!");
    private final Tooltip streakTip = new Tooltip();
    private final Timeline clock;
    private String userName = "";
    private LocalDate shownDate;
    private Runnable onNewDay = () -> { };

    public HeaderBar(PlanStore store, List<String> pageNames, Consumer<String> onOpen, Runnable onLogout) {
        this(store, pageNames, onOpen, onLogout, LocalDateTime::now);
    }

    public HeaderBar(PlanStore store, List<String> pageNames, Consumer<String> onOpen, Runnable onLogout,
                     Supplier<LocalDateTime> now) {
        this.store = store;
        this.now = now;
        HBox pillRow = new HBox(6);
        pageNames.forEach(name -> pillRow.getChildren().add(pill(name, onOpen)));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        avatar.getStyleClass().add("pnp-avatar");
        Button logout = new Button("Log out");
        logout.setId("headerLogout");
        logout.getStyleClass().add("pnp-logout");
        logout.setOnAction(e -> onLogout.run());
        HBox account = new HBox(10, avatar, logout);
        account.setAlignment(Pos.CENTER_LEFT);
        HBox nav = new HBox(20, Theme.logo(44), pillRow, gap, account);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.getStyleClass().add("pnp-nav");
        pillRow.setAlignment(Pos.CENTER_LEFT);

        greetingLabel.setId("headerGreeting");
        greetingLabel.getStyleClass().add("pnp-hero-greeting");
        pageTitle.setId("headerTitle");
        pageTitle.getStyleClass().add("pnp-hero-title");
        pageSubtitle.setId("headerSubtitle");
        pageSubtitle.getStyleClass().add("pnp-hero-subtitle");
        pageSubtitle.setWrapText(true);
        streakChip.setId("streakChip");
        streakChip.getStyleClass().add("pnp-streak-chip");
        streakChip.setGraphic(flame);
        streakChip.setGraphicTextGap(7);
        badgeChip.setId("badgeChip");
        badgeChip.getStyleClass().add("pnp-streak-chip");
        badgeChip.setGraphic(Theme.icon("hat", 18));
        badgeChip.setGraphicTextGap(7);
        hintLabel.getStyleClass().add("pnp-badge");
        Theme.show(hintLabel, false);
        Tooltip.install(streakChip, streakTip);
        Tooltip.install(badgeChip, streakTip);
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        VBox words = new VBox(3, greetingLabel, pageTitle, pageSubtitle);
        words.setAlignment(Pos.CENTER_LEFT);
        words.setMaxWidth(640);
        HBox chips = new HBox(10, streakChip, badgeChip);
        chips.setAlignment(Pos.CENTER_RIGHT);
        VBox right = new VBox(8, chips, hintLabel);
        right.setAlignment(Pos.TOP_RIGHT);
        HBox hello = new HBox(12, words, gap2, right);
        hello.setAlignment(Pos.TOP_LEFT);
        hello.getStyleClass().add("pnp-hero-body");
        StackPane banner = new StackPane(Theme.waves(0.4), hello);   // the waves sit under the text
        banner.getStyleClass().add("pnp-hero");                       // gradient background on the stack
        banner.setAlignment(Pos.TOP_LEFT);
        flamePulse.setFromX(1.0);                           // the flame beats gently, all the time
        flamePulse.setFromY(1.0);
        flamePulse.setToX(1.18);
        flamePulse.setToY(1.18);
        flamePulse.setAutoReverse(true);
        flamePulse.setCycleCount(Animation.INDEFINITE);
        flamePulse.setInterpolator(Interpolator.EASE_BOTH);
        flamePulse.play();

        root.getChildren().addAll(nav, banner);
        Theme.apply(root);
        tick();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public Parent getView() {
        return root;
    }

    /** Marks a page's pill as the selected one (without clicking it) and puts its title in the banner. */
    public void select(String pageName) {
        String[] titles = TITLES.get(pageName);
        String newTitle = titles == null ? pageName : titles[0];
        if (!newTitle.equals(pageTitle.getText())) {       // a new page: its title fades in
            pageTitle.setText(newTitle);
            pageSubtitle.setText(titles == null ? "" : titles[1]);
            fadeIn(pageTitle);
            fadeIn(pageSubtitle);
        }
        pills.getToggles().stream()
                .filter(toggle -> pageName.equals(toggle.getUserData()))
                .findFirst()
                .ifPresent(toggle -> toggle.setSelected(true));
    }

    public void setUserName(String name) {
        userName = name == null ? "" : name.trim();
        avatar.setText(userName.isEmpty() ? "?" : userName.substring(0, 1).toUpperCase());
        tick();
    }

    public void setOnNewDay(Runnable onNewDay) {
        this.onNewDay = onNewDay;
    }

    public void stop() {
        clock.stop();
        flamePulse.stop();
    }

    private static void fadeIn(Node node) {
        FadeTransition fade = new FadeTransition(Duration.millis(320), node);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    private ToggleButton pill(String name, Consumer<String> onOpen) {
        ToggleButton pill = new ToggleButton(name);
        pill.setId("nav-" + name.replace(" ", ""));
        pill.setUserData(name);
        pill.setToggleGroup(pills);
        pill.getStyleClass().add("pnp-nav-pill");
        pill.setOnAction(e -> onOpen.accept(name));     // AppShell opens the page and keeps the pill selected
        return pill;
    }

    private void tick() {
        LocalDateTime moment = now.get();
        String who = userName.isEmpty() ? "" : ", " + userName;
        greetingLabel.setText(TextFormat.greeting(moment.toLocalTime()) + who + " · "
                + TextFormat.date(moment.toLocalDate()) + " · " + TextFormat.time(moment.toLocalTime()));
        if (shownDate != null && !shownDate.equals(moment.toLocalDate())) {
            onNewDay.run();                                // a new day: Today and the streak change
        }
        shownDate = moment.toLocalDate();
    }

    /** Reads the streak in the background and shows it. */
    public void refresh() {
        LocalDate today = now.get().toLocalDate();
        Task<StreakData> task = new Task<>() {
            @Override
            protected StreakData call() throws Exception {
                Set<LocalDate> days = store.plannedDates();
                LocalDate[] range = StreakCalculator.currentRange(days, today);
                return new StreakData(days, range == null ? List.of() : store.mealsBetween(range[0], range[1]));
            }
        };
        task.setOnSucceeded(e -> showStreak(today, task.getValue()));
        task.setOnFailed(e -> streakTip.setText("Could not read the meal plan."));
        AppShell.runInBackground(task);
    }

    private void showStreak(LocalDate today, StreakData data) {
        int current = StreakCalculator.current(data.days(), today);
        int best = StreakCalculator.longest(data.days(), today);
        boolean atRisk = StreakCalculator.atRisk(data.days(), today);
        Map<PrepTier, Long> tiers = StreakCalculator.tierCounts(data.streakMeals());
        streakChip.setText(current + "-day streak");
        streakChip.getStyleClass().remove("at-risk");
        if (atRisk) {
            streakChip.getStyleClass().add("at-risk");
        }
        Theme.show(hintLabel, atRisk);
        badgeChip.setText(current == 0 ? "Plan a meal to start" : StreakCalculator.badgeTitle(tiers) + " badge");
        streakTip.setText("Current streak: " + current + (current == 1 ? " day" : " days")
                + "\nBest streak: " + best + (best == 1 ? " day" : " days")
                + "\nNext goal: " + StreakCalculator.goalLine(current)
                + (current == 0 ? "" : "\nBadge: " + StreakCalculator.badgeTitle(tiers) + ", because "
                        + StreakCalculator.badgeReason(tiers))
                + "\nMeals in this streak: " + StreakCalculator.tierLine(tiers)
                + "\nPlan at least one meal every day to keep it going.");
    }
}
