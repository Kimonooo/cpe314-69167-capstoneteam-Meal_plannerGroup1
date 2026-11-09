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
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * The top of every page, like the header of a website:
 *   row 1: the logo, one rounded pill per page, the user's initials and Log out;
 *   row 2: the greeting with the date and time, the streak and the prep-tier badge.
 * The clock ticks every second; at midnight it refreshes the streak and tells the other pages.
 */
public class HeaderBar {
    private record StreakData(Set<LocalDate> days, List<PlannedMeal> streakMeals) { }

    private final PlanStore store;
    private final Supplier<LocalDateTime> now;
    private final VBox root = new VBox();
    private final ToggleGroup pills = new ToggleGroup();
    private final Label avatar = new Label();
    private final Label greetingLabel = new Label();
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
        HBox pillRow = new HBox(4);
        pageNames.forEach(name -> pillRow.getChildren().add(pill(name, onOpen)));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        avatar.getStyleClass().add("pnp-avatar");
        Button logout = new Button("Log out");
        logout.setId("headerLogout");
        logout.getStyleClass().add("pnp-link");
        logout.setOnAction(e -> onLogout.run());
        HBox nav = new HBox(18, Theme.logo(46), pillRow, gap, avatar, logout);
        nav.setAlignment(Pos.CENTER_LEFT);
        nav.getStyleClass().add("pnp-nav");
        pillRow.setAlignment(Pos.CENTER_LEFT);

        greetingLabel.setId("headerGreeting");
        greetingLabel.getStyleClass().add("pnp-subtitle");
        streakChip.setId("streakChip");
        streakChip.getStyleClass().add("pnp-streak-chip");
        badgeChip.setId("badgeChip");
        badgeChip.getStyleClass().add("pnp-streak-chip");
        hintLabel.getStyleClass().add("pnp-badge");
        Theme.show(hintLabel, false);
        Tooltip.install(streakChip, streakTip);
        Region gap2 = new Region();
        HBox.setHgrow(gap2, Priority.ALWAYS);
        HBox hello = new HBox(8, greetingLabel, gap2, hintLabel, streakChip, badgeChip);
        hello.setAlignment(Pos.CENTER_LEFT);
        hello.getStyleClass().add("pnp-hello");

        root.getChildren().addAll(nav, hello);
        Theme.apply(root);
        tick();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public Parent getView() {
        return root;
    }

    /** Marks a page's pill as the selected one (without clicking it). */
    public void select(String pageName) {
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
                + "\nMeals in this streak: " + StreakCalculator.tierLine(tiers)
                + "\nPlan at least one meal every day to keep it going.");
    }
}
