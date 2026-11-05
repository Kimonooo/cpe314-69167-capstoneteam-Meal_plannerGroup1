package com.platenplatter.ui;

import com.platenplatter.util.DateTimeText;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;
import com.platenplatter.util.StreakCalculator;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The "Today" tab: the home page.
 * Shows the streak, the best streak, how many meals are planned today, the 7 days of this week
 * (gold = a meal is planned) and today's meals. Everything is loaded in the background.
 * Controls that work: Find a recipe, Open shopping list.
 */
public class TodayScreen {
    private final PlanStore store;
    private final Supplier<LocalDate> todayProvider;

    private final VBox root = new VBox(14);
    private final Label dateLabel = new Label();
    private final Label streakValue = new Label("0");
    private final Label streakNote = new Label();
    private final Label bestValue = new Label("0");
    private final Label mealsValue = new Label("0");
    private final HBox weekStrip = new HBox(10);
    private final VBox mealsBox = new VBox(8);
    private final Label mealsTitle = new Label("Today's meals");

    public TodayScreen(PlanStore store, Runnable goRecipes, Runnable goShopping) {
        this(store, goRecipes, goShopping, LocalDate::now);
    }

    public TodayScreen(PlanStore store, Runnable goRecipes, Runnable goShopping, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.todayProvider = todayProvider;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildStats(), buildWeekCard(), buildMealsCard(goRecipes, goShopping));
        Theme.apply(root);
        refresh();
    }

    public Parent getView() {
        return root;
    }

    /** Re-reads the plan and redraws everything (off the UI thread). */
    public void refresh() {
        LocalDate today = todayProvider.get();
        dateLabel.setText(DateTimeText.date(today));

        Task<Object[]> task = new Task<>() {
            @Override
            protected Object[] call() throws Exception {
                Set<LocalDate> days = store.plannedDates();
                List<PlannedMeal> meals = store.mealsOn(today);
                return new Object[] {days, meals};
            }
        };
        task.setOnSucceeded(e -> {
            @SuppressWarnings("unchecked") Set<LocalDate> days = (Set<LocalDate>) task.getValue()[0];
            @SuppressWarnings("unchecked") List<PlannedMeal> meals = (List<PlannedMeal>) task.getValue()[1];
            show(today, days, meals);
        });
        task.setOnFailed(e -> {
            Label failed = new Label("Could not read the meal plan.");
            failed.getStyleClass().addAll("pnp-status", "error");
            mealsBox.getChildren().setAll(failed);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------------ building the screen

    private VBox buildHeader() {
        Label title = new Label("Today");
        title.getStyleClass().add("pnp-title");
        dateLabel.getStyleClass().add("pnp-subtitle");
        dateLabel.setId("todayDate");
        return new VBox(2, title, dateLabel);
    }

    private HBox buildStats() {
        streakValue.setId("todayStreak");
        streakNote.getStyleClass().add("pnp-muted");
        streakNote.setWrapText(true);
        streakNote.setId("todayStreakNote");
        mealsValue.setId("todayMeals");
        bestValue.setId("todayBest");

        VBox streakCard = statCard("CURRENT STREAK", streakValue, "days in a row");
        streakCard.getChildren().add(streakNote);
        HBox row = new HBox(14, streakCard, statCard("BEST STREAK", bestValue, "days in a row"),
                statCard("MEALS TODAY", mealsValue, "planned"));
        return row;
    }

    private VBox statCard(String caption, Label value, String unit) {
        value.getStyleClass().add("pnp-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pnp-stat-label");
        Label un = new Label(unit);
        un.getStyleClass().add("pnp-muted");
        VBox card = new VBox(2, cap, value, un);
        card.getStyleClass().add("pnp-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(10);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildWeekCard() {
        Label title = new Label("This week");
        title.getStyleClass().add("pnp-section");
        Label hint = new Label("Gold means at least one meal is planned that day.");
        hint.getStyleClass().add("pnp-muted");
        weekStrip.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(10, title, weekStrip, hint);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildMealsCard(Runnable goRecipes, Runnable goShopping) {
        mealsTitle.getStyleClass().add("pnp-section");
        Button find = new Button("Find a recipe");
        find.setId("todayFindRecipe");
        find.getStyleClass().add("pnp-button");
        find.setOnAction(e -> goRecipes.run());
        Button shop = new Button("Open shopping list");
        shop.setId("todayShopping");
        shop.getStyleClass().add("pnp-button-secondary");
        shop.setOnAction(e -> goShopping.run());
        HBox actions = new HBox(10, find, shop);

        VBox card = new VBox(10, mealsTitle, mealsBox, actions);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    // ------------------------------------------------------------------ showing the result

    private void show(LocalDate today, Set<LocalDate> days, List<PlannedMeal> meals) {
        int current = StreakCalculator.current(days, today);
        streakValue.setText(String.valueOf(current));
        bestValue.setText(String.valueOf(StreakCalculator.longest(days, today)));
        mealsValue.setText(String.valueOf(meals.size()));
        if (StreakCalculator.atRisk(days, today)) {
            streakNote.setText("Plan a meal today to keep it going!");
        } else if (current == 0) {
            streakNote.setText("Plan a meal today to start a streak.");
        } else {
            streakNote.setText("Great job, today is covered.");
        }

        weekStrip.getChildren().clear();
        boolean[] flags = StreakCalculator.weekFlags(days, today);
        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - 1L);
        for (int i = 0; i < 7; i++) {
            LocalDate day = monday.plusDays(i);
            Label name = new Label(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH).toUpperCase());
            name.getStyleClass().add("pnp-week-label");
            Label circle = new Label(String.valueOf(day.getDayOfMonth()));
            circle.getStyleClass().add("pnp-weekday");
            if (flags[i]) {
                circle.getStyleClass().add("has-meals");
            }
            if (day.equals(today)) {
                circle.getStyleClass().add("is-today");
            }
            VBox cell = new VBox(4, name, circle);
            cell.setAlignment(Pos.CENTER);
            weekStrip.getChildren().add(cell);
        }

        mealsTitle.setText(meals.isEmpty() ? "Today's meals" : "Today's meals (" + meals.size() + ")");
        mealsBox.getChildren().clear();
        if (meals.isEmpty()) {
            Label none = new Label("Nothing is planned for today yet. Find a recipe and add it to your plan.");
            none.getStyleClass().add("pnp-empty");
            none.setWrapText(true);
            mealsBox.getChildren().add(none);
        } else {
            for (PlannedMeal meal : meals) {
                mealsBox.getChildren().add(MealRows.row(meal, null));
            }
        }
    }
}
