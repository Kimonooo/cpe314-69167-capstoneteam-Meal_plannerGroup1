package com.platenplatter.ui;

import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import com.platenplatter.service.StatisticsService;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The "Today" tab: the home page.
 * Shows the streak with its badge (Quick Cook, Home Cook or Slow-Food Chef, from the prep times of the
 * streak's meals), the best streak, how many meals and calories are planned today, the 7 days of this
 * week (gold = a meal is planned) and today's meals grouped by Breakfast, Lunch, Dinner and Snack.
 * An empty Breakfast, Lunch or Dinner has an "Add" button; Snack is only shown when one is planned.
 * Everything is loaded in the background.
 * Controls that work: the Add buttons, Find a recipe, Open shopping list.
 */
public class TodayScreen {
    private final PlanStore store;
    private final Supplier<LocalDate> todayProvider;
    private final Consumer<MealSlot> addMealFor;

    private final VBox root = new VBox(14);
    /** The page scrolls when the window is shorter than it, so nothing at the bottom is cut off. */
    private final ScrollPane scroller = new ScrollPane(root);
    private final Label dateLabel = new Label();
    private final Label streakValue = new Label("0");
    private final Label streakNote = new Label();
    private final Label badgeLabel = new Label();
    private final Label bestValue = new Label("0");
    private final Label mealsValue = new Label("0");
    private final Label mealsUnit = new Label("planned");
    private final HBox weekStrip = new HBox(10);
    private final VBox mealsBox = new VBox(10);
    private final Label mealsTitle = new Label("Today's meals");

    /** What one refresh reads from the database. */
    private static final class TodayData {
        final Set<LocalDate> days;
        final List<PlannedMeal> todayMeals;
        final List<PlannedMeal> streakMeals;

        TodayData(Set<LocalDate> days, List<PlannedMeal> todayMeals, List<PlannedMeal> streakMeals) {
            this.days = days;
            this.todayMeals = todayMeals;
            this.streakMeals = streakMeals;
        }
    }

    /** Old constructor: every "Add" button just opens the Recipes tab. */
    public TodayScreen(PlanStore store, Runnable goRecipes, Runnable goShopping) {
        this(store, slot -> goRecipes.run(), goShopping, LocalDate::now);
    }

    /** @param addMealFor opens the Recipes tab with this slot picked (Breakfast, Lunch, Dinner or Snack) */
    public TodayScreen(PlanStore store, Consumer<MealSlot> addMealFor, Runnable goShopping) {
        this(store, addMealFor, goShopping, LocalDate::now);
    }

    public TodayScreen(PlanStore store, Consumer<MealSlot> addMealFor, Runnable goShopping, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.addMealFor = addMealFor;
        this.todayProvider = todayProvider;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildStats(), buildWeekCard(), buildMealsCard(goShopping));
        Theme.apply(root);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        refresh();
    }

    public Parent getView() {
        return scroller;
    }

    /** Re-reads the plan and redraws everything (off the UI thread). */
    public void refresh() {
        LocalDate today = todayProvider.get();
        dateLabel.setText(DateTimeText.date(today));

        Task<TodayData> task = new Task<>() {
            @Override
            protected TodayData call() throws Exception {
                Set<LocalDate> days = store.plannedDates();
                List<PlannedMeal> meals = store.mealsOn(today);
                LocalDate[] range = StreakCalculator.currentRange(days, today);
                List<PlannedMeal> streakMeals = range == null ? new ArrayList<>() : store.mealsBetween(range[0], range[1]);
                return new TodayData(days, meals, streakMeals);
            }
        };
        task.setOnSucceeded(e -> show(today, task.getValue()));
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
        badgeLabel.setId("todayBadge");
        badgeLabel.getStyleClass().add("pnp-badge");
        badgeLabel.setWrapText(true);
        mealsValue.setId("todayMeals");
        mealsUnit.setId("todayCalories");
        bestValue.setId("todayBest");

        VBox streakCard = statCard("CURRENT STREAK", streakValue, new Label("days in a row"));
        streakCard.getChildren().addAll(badgeLabel, streakNote);
        HBox row = new HBox(14, streakCard, statCard("BEST STREAK", bestValue, new Label("days in a row")),
                statCard("MEALS TODAY", mealsValue, mealsUnit));
        return row;
    }

    private VBox statCard(String caption, Label value, Label unit) {
        value.getStyleClass().add("pnp-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pnp-stat-label");
        unit.getStyleClass().add("pnp-muted");
        VBox card = new VBox(2, cap, value, unit);
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

    private VBox buildMealsCard(Runnable goShopping) {
        mealsTitle.getStyleClass().add("pnp-section");
        Button find = new Button("Find a recipe");
        find.setId("todayFindRecipe");
        find.getStyleClass().add("pnp-button");
        find.setOnAction(e -> addMealFor.accept(null));
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

    private void show(LocalDate today, TodayData data) {
        int current = StreakCalculator.current(data.days, today);
        streakValue.setText(String.valueOf(current));
        bestValue.setText(String.valueOf(StreakCalculator.longest(data.days, today)));
        mealsValue.setText(String.valueOf(data.todayMeals.size()));
        int calories = StatisticsService.totalCalories(data.todayMeals);
        mealsUnit.setText(calories > 0 ? String.format("planned, %,d kcal", calories) : "planned");
        if (StreakCalculator.atRisk(data.days, today)) {
            streakNote.setText("Plan a meal today to keep it going!");
        } else if (current == 0) {
            streakNote.setText("Plan a meal today to start a streak.");
        } else {
            streakNote.setText("Great job, today is covered.");
        }
        Map<PrepTier, Integer> tiers = StreakCalculator.tierCounts(data.streakMeals);
        badgeLabel.setText(current == 0 ? "" : StreakCalculator.badgeTitle(tiers) + " badge: " + StreakCalculator.tierLine(tiers));
        badgeLabel.setVisible(current > 0);
        badgeLabel.setManaged(current > 0);

        drawWeek(today, data.days);
        drawMeals(data.todayMeals);
    }

    private void drawWeek(LocalDate today, Set<LocalDate> days) {
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
    }

    /** Today's meals under Breakfast, Lunch, Dinner and Snack. */
    private void drawMeals(List<PlannedMeal> meals) {
        mealsTitle.setText(meals.isEmpty() ? "Today's meals" : "Today's meals (" + meals.size() + ")");
        mealsBox.getChildren().clear();
        for (MealSlot slot : MealSlot.values()) {
            List<PlannedMeal> inSlot = new ArrayList<>();
            for (PlannedMeal meal : meals) {
                if (meal.getMealSlot() == slot) {
                    inSlot.add(meal);
                }
            }
            if (slot == MealSlot.SNACK && inSlot.isEmpty()) {
                continue; // a snack is optional, so an empty Snack is not shown
            }
            Label heading = new Label(slot.getLabel());
            heading.getStyleClass().add("pnp-slot-heading");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox headRow = new HBox(8, heading, spacer);
            headRow.setAlignment(Pos.CENTER_LEFT);
            VBox section = new VBox(6, headRow);
            if (inSlot.isEmpty()) {
                Label none = new Label("Nothing planned");
                none.getStyleClass().add("pnp-empty");
                Button add = new Button("Add " + slot.getLabel().toLowerCase());
                add.setId("todayAdd" + slot.getLabel());
                add.getStyleClass().add("pnp-small-add");
                add.setOnAction(e -> addMealFor.accept(slot));
                headRow.getChildren().add(add);
                section.getChildren().add(none);
            } else {
                for (PlannedMeal meal : inSlot) {
                    section.getChildren().add(MealRows.row(meal, null, false));
                }
            }
            section.getStyleClass().add("pnp-slot-section");
            mealsBox.getChildren().add(section);
        }
    }
}
