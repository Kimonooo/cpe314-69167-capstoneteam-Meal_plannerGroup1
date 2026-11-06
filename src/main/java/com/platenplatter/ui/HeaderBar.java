package com.platenplatter.ui;

import com.platenplatter.model.PrepTier;
import com.platenplatter.util.DateTimeText;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;
import com.platenplatter.util.StreakCalculator;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The maroon band at the top of the app.
 * It shows the greeting with the user's display name, the REAL-TIME date and clock (updated every
 * second by a Timeline, so it never needs a button) and the STREAK badge: how many days in a row
 * have a planned meal, and how the streak's meals split into Quick, Average and Slow.
 * The streak is re-counted from the database whenever refresh() is called (when a meal is added,
 * removed or imported) and automatically when the clock passes midnight.
 */
public class HeaderBar {
    private final PlanStore store;
    private final Supplier<LocalDateTime> now;

    private final HBox root = new HBox(18);
    private final Label greetingLabel = new Label();
    private final Label dateLabel = new Label();
    private final Label timeLabel = new Label();
    private final Label hintLabel = new Label();
    private final Label streakValue = new Label("0");
    private final Label streakCaption = new Label("DAY STREAK");
    private final Label streakTiers = new Label();
    private final VBox streakPill = new VBox(0, streakValue, streakCaption, streakTiers);
    private final Tooltip streakTip = new Tooltip();
    private final Button logoutButton = new Button("Log out");
    private final Timeline clock;

    private String userName = "";
    private LocalDate shownDate;
    private Runnable onNewDay = () -> { };

    /** What one streak re-count finds: the planned days, and the meals inside the current streak. */
    private static final class StreakData {
        final Set<LocalDate> days;
        final List<PlannedMeal> streakMeals;

        StreakData(Set<LocalDate> days, List<PlannedMeal> streakMeals) {
            this.days = days;
            this.streakMeals = streakMeals;
        }
    }

    public HeaderBar(PlanStore store) {
        this(store, LocalDateTime::now);
    }

    /** The second constructor lets a test pretend it is any time of day. */
    public HeaderBar(PlanStore store, Supplier<LocalDateTime> now) {
        this.store = store;
        this.now = now;

        Label title = new Label("Plate N Platter");
        title.getStyleClass().add("pnp-header-title");
        greetingLabel.getStyleClass().add("pnp-header-greeting");
        greetingLabel.setId("headerGreeting");
        VBox left = new VBox(2, title, greetingLabel);

        dateLabel.getStyleClass().add("pnp-header-date");
        dateLabel.setId("headerDate");
        timeLabel.getStyleClass().add("pnp-header-time");
        timeLabel.setId("headerTime");
        hintLabel.getStyleClass().add("pnp-header-greeting");
        hintLabel.setId("headerHint");
        hintLabel.setStyle("-fx-text-fill: #FFD84D;");
        VBox clockBox = new VBox(0, dateLabel, timeLabel, hintLabel);
        clockBox.setAlignment(Pos.CENTER_RIGHT);

        streakValue.getStyleClass().add("pnp-streak-value");
        streakValue.setId("streakValue");
        streakCaption.getStyleClass().add("pnp-streak-caption");
        streakCaption.setId("streakCaption");
        streakTiers.getStyleClass().add("pnp-streak-tiers");
        streakTiers.setId("streakTiers");
        streakPill.getStyleClass().add("pnp-streak");
        streakPill.setId("streakPill");
        Tooltip.install(streakPill, streakTip);

        logoutButton.setId("headerLogout");
        logoutButton.getStyleClass().add("pnp-header-button");
        logoutButton.setVisible(false);
        logoutButton.setManaged(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        root.getChildren().addAll(left, spacer, clockBox, streakPill, logoutButton);
        root.setAlignment(Pos.CENTER_LEFT);
        root.getStyleClass().add("pnp-header");
        Theme.apply(root);

        tick();
        refresh();
        clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> tick()));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    public Parent getView() {
        return root;
    }

    /** The display name used in the greeting ("Good morning, Jon!"). Call again after a rename. */
    public void setUserName(String name) {
        this.userName = name == null ? "" : name.trim();
        tick();
    }

    /** Shows the Log out button; null hides it (previews have nobody to log out). */
    public void setOnLogout(Runnable onLogout) {
        boolean show = onLogout != null;
        logoutButton.setVisible(show);
        logoutButton.setManaged(show);
        logoutButton.setOnAction(show ? e -> onLogout.run() : null);
    }

    /** Called when the clock passes midnight, so other tabs can show the new day. */
    public void setOnNewDay(Runnable onNewDay) {
        this.onNewDay = onNewDay == null ? () -> { } : onNewDay;
    }

    /** Stops the clock (call when the window closes or the user logs out). */
    public void stop() {
        clock.stop();
    }

    /** Runs once a second: updates the date, the clock and the greeting. */
    private void tick() {
        LocalDateTime moment = now.get();
        dateLabel.setText(DateTimeText.date(moment.toLocalDate()));
        timeLabel.setText(DateTimeText.time(moment.toLocalTime()));
        String who = userName.isEmpty() ? "" : ", " + userName;
        greetingLabel.setText(DateTimeText.greeting(moment.toLocalTime()) + who + "! What are we cooking?");
        if (shownDate != null && !shownDate.equals(moment.toLocalDate())) {
            shownDate = moment.toLocalDate();
            refresh();
            onNewDay.run();
        }
        shownDate = moment.toLocalDate();
    }

    /** Re-counts the streak and its prep-time tiers from the database (in the background). */
    public void refresh() {
        LocalDate today = now.get().toLocalDate();
        Task<StreakData> task = new Task<>() {
            @Override
            protected StreakData call() throws Exception {
                Set<LocalDate> days = store.plannedDates();
                LocalDate[] range = StreakCalculator.currentRange(days, today);
                List<PlannedMeal> meals = range == null ? new ArrayList<>() : store.mealsBetween(range[0], range[1]);
                return new StreakData(days, meals);
            }
        };
        task.setOnSucceeded(e -> showStreak(today, task.getValue()));
        task.setOnFailed(e -> {
            streakValue.setText("-");
            streakTiers.setText("");
            streakTip.setText("Could not read the meal plan.");
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void showStreak(LocalDate today, StreakData data) {
        int current = StreakCalculator.current(data.days, today);
        int best = StreakCalculator.longest(data.days, today);
        boolean atRisk = StreakCalculator.atRisk(data.days, today);
        Map<PrepTier, Integer> tiers = StreakCalculator.tierCounts(data.streakMeals);

        streakValue.setText(String.valueOf(current));
        streakTiers.setText(current == 0 ? "plan a meal to start" : StreakCalculator.tierLine(tiers));

        streakPill.getStyleClass().remove("at-risk");
        if (atRisk) {
            streakPill.getStyleClass().add("at-risk");
        }
        hintLabel.setText(atRisk ? "Plan a meal today to keep your streak!" : "");
        hintLabel.setVisible(atRisk);
        hintLabel.setManaged(atRisk);
        streakTip.setText("Badge: " + StreakCalculator.badgeTitle(tiers)
                + "\nCurrent streak: " + current + (current == 1 ? " day" : " days")
                + "\nBest streak: " + best + (best == 1 ? " day" : " days")
                + "\n\nMeals in this streak by prep time:"
                + "\n  Quick (" + PrepTier.QUICK.getRange() + "): " + tiers.get(PrepTier.QUICK)
                + "\n  Average (" + PrepTier.AVERAGE.getRange() + "): " + tiers.get(PrepTier.AVERAGE)
                + "\n  Slow (" + PrepTier.SLOW.getRange() + "): " + tiers.get(PrepTier.SLOW)
                + "\n\nPlan at least one meal every day to keep it going.");
    }
}
