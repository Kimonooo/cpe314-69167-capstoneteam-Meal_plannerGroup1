package com.platenplatter.ui;

import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.StreakCalculator;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The Today page: the streak, this week's planned days, and today's meals under
 * Breakfast, Lunch and Dinner (and Snack when there is one). An empty meal has an Add button
 * that opens Recipes with that slot already picked.
 */
public class TodayScreen {
    private record TodayData(Set<LocalDate> days, List<PlannedMeal> todayMeals, List<PlannedMeal> streakMeals) { }

    private final PlanStore store;
    private final Consumer<MealSlot> addMealFor;
    private final Supplier<LocalDate> todayProvider;
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final Label streakValue = new Label("0");
    private final Label streakNote = new Label();
    private final Label bestValue = new Label("0");
    private final Label mealsValue = new Label("0");
    private final Label mealsUnit = new Label("planned");
    private final HBox weekStrip = new HBox(10);
    private final VBox mealsBox = new VBox(4);

    public TodayScreen(PlanStore store, Consumer<MealSlot> addMealFor, Runnable goShopping) {
        this(store, addMealFor, goShopping, LocalDate::now);
    }

    public TodayScreen(PlanStore store, Consumer<MealSlot> addMealFor, Runnable goShopping, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.addMealFor = addMealFor;
        this.todayProvider = todayProvider;
        streakValue.setId("todayStreak");
        bestValue.setId("todayBest");
        mealsValue.setId("todayMeals");
        streakNote.setId("todayStreakNote");
        Label streakUnit = new Label("days in a row");
        HBox stats = new HBox(14, Theme.statCard("CURRENT STREAK", streakValue, streakUnit),
                Theme.statCard("BEST STREAK", bestValue, new Label("days in a row")),
                Theme.statCard("MEALS TODAY", mealsValue, mealsUnit));
        ((VBox) stats.getChildren().get(0)).getChildren().add(streakNote);
        streakNote.getStyleClass().add("pnp-badge");

        Button find = new Button("Find a recipe");
        find.setId("todayFindRecipe");
        find.getStyleClass().add("pnp-button");
        find.setOnAction(e -> addMealFor.accept(null));
        Button shop = new Button("Open shopping list");
        shop.setId("todayShopping");
        shop.getStyleClass().add("pnp-button-secondary");
        shop.setOnAction(e -> goShopping.run());
        VBox week = card(Theme.text("This week", "pnp-section"), weekStrip,
                Theme.text("Maroon means at least one meal is planned that day.", "pnp-muted"));
        VBox meals = card(Theme.text("Today's meals", "pnp-section"), mealsBox, new HBox(10, find, shop));

        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(Theme.pageHeader("Today", "Your streak and what is planned for today."),
                stats, week, meals);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        refresh();
    }

    public Parent getView() {
        return scroller;
    }

    public void refresh() {
        LocalDate today = todayProvider.get();
        Task<TodayData> task = new Task<>() {
            @Override
            protected TodayData call() throws Exception {
                Set<LocalDate> days = store.plannedDates();
                LocalDate[] range = StreakCalculator.currentRange(days, today);
                return new TodayData(days, store.mealsOn(today),
                        range == null ? List.of() : store.mealsBetween(range[0], range[1]));
            }
        };
        task.setOnSucceeded(e -> show(today, task.getValue()));
        task.setOnFailed(e -> mealsBox.getChildren().setAll(Theme.text("Could not read the meal plan.", "pnp-empty")));
        AppShell.runInBackground(task);
    }

    private static VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void show(LocalDate today, TodayData data) {
        int current = StreakCalculator.current(data.days(), today);
        streakValue.setText(String.valueOf(current));
        bestValue.setText(String.valueOf(StreakCalculator.longest(data.days(), today)));
        mealsValue.setText(String.valueOf(data.todayMeals().size()));
        int calories = StatisticsService.totalCalories(data.todayMeals());
        mealsUnit.setText(calories > 0 ? "planned, " + TextFormat.calories(calories) : "planned");
        Map<PrepTier, Long> tiers = StreakCalculator.tierCounts(data.streakMeals());
        streakNote.setText(StreakCalculator.atRisk(data.days(), today) ? "Plan a meal today to keep it going!"
                : current == 0 ? "Plan a meal today to start a streak."
                : StreakCalculator.badgeTitle(tiers) + " badge: " + StreakCalculator.tierLine(tiers));
        drawWeek(today, data.days());
        mealsBox.getChildren().setAll(Arrays.stream(MealSlot.values())
                .map(slot -> slotSection(slot, data.todayMeals().stream().filter(m -> m.getMealSlot() == slot).toList()))
                .filter(Objects::nonNull)
                .toList());
    }

    private void drawWeek(LocalDate today, Set<LocalDate> days) {
        List<Boolean> flags = StreakCalculator.weekFlags(days, today);
        LocalDate monday = StatisticsService.mondayOf(today);
        weekStrip.getChildren().setAll(IntStream.range(0, 7).mapToObj(i -> {
            LocalDate day = monday.plusDays(i);
            Label circle = Theme.text(String.valueOf(day.getDayOfMonth()), "pnp-weekday");
            if (flags.get(i)) {
                circle.getStyleClass().add("has-meals");
            }
            if (day.equals(today)) {
                circle.getStyleClass().add("is-today");
            }
            VBox cell = new VBox(4, Theme.text(day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    .toUpperCase(), "pnp-week-label"), circle);
            cell.setAlignment(Pos.CENTER);
            return cell;
        }).toList());
    }

    /** One slot with its meals, or an Add button when it is empty. An empty Snack is not shown (null). */
    private VBox slotSection(MealSlot slot, List<PlannedMeal> meals) {
        if (slot == MealSlot.SNACK && meals.isEmpty()) {
            return null;
        }
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(8, Theme.text(slot.getLabel(), "pnp-slot-heading"), gap);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox section = new VBox(6, head);
        if (meals.isEmpty()) {
            Button add = new Button("Add " + slot.getLabel().toLowerCase());
            add.setId("todayAdd" + slot.getLabel());
            add.getStyleClass().add("pnp-small-add");
            add.setOnAction(e -> addMealFor.accept(slot));
            head.getChildren().add(add);
            section.getChildren().add(Theme.text("Nothing planned", "pnp-empty"));
        } else {
            meals.forEach(meal -> section.getChildren().add(MealRows.row(meal, null, false)));
        }
        section.getStyleClass().add("pnp-slot-section");
        return section;
    }
}
