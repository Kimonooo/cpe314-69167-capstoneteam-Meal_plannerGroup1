package com.platenplatter.ui;

import com.platenplatter.api.MealData;
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
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The Today page. The streak card on top shows the current streak as a big number, the badge
 * it earned, a bar that fills toward the next goal (3, 7, 14, 30 ... days) and this week's
 * planned days. Next to it are the best streak and today's meal count. Then comes the featured
 * meal: one of today's picks from TheMealDB with its large photo, a "Plan this meal" button and
 * three more ideas. Below are today's meals under Breakfast, Lunch and Dinner (and Snack when
 * there is one), each with its photo. An empty meal has an Add button that opens Recipes with
 * that slot already picked.
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
    private final Label streakUnit = new Label("days in a row");
    private final Label badgeChip = new Label();
    private final ProgressBar goalBar = new ProgressBar(0);
    private final Label goalLabel = new Label();
    private final VBox streakCard = new VBox(8);
    private final Label bestValue = new Label("0");
    private final Label mealsValue = new Label("0");
    private final Label mealsUnit = new Label("planned");
    private final HBox weekStrip = new HBox(10);
    private final VBox mealsBox = new VBox(4);
    private final VBox featuredCard = new VBox(12);
    private Consumer<MealData> onPlanFeatured = meal -> { };

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
        VBox best = Theme.statCard("BEST STREAK", bestValue, new Label("days in a row"), "trophy");
        VBox mealsToday = Theme.statCard("MEALS TODAY", mealsValue, mealsUnit, "dishes");
        VBox.setVgrow(best, Priority.ALWAYS);
        VBox.setVgrow(mealsToday, Priority.ALWAYS);
        VBox side = new VBox(14, best, mealsToday);
        side.setPrefWidth(250);
        side.setMinWidth(210);
        HBox stats = new HBox(14, buildStreakCard(), side);

        Button find = new Button("Find a recipe");
        find.setId("todayFindRecipe");
        find.getStyleClass().add("pnp-button");
        find.setOnAction(e -> addMealFor.accept(null));
        Button shop = new Button("Open shopping list");
        shop.setId("todayShopping");
        shop.getStyleClass().add("pnp-button-secondary");
        shop.setOnAction(e -> goShopping.run());
        Label mealsTitle = Theme.text("Today's meals", "pnp-section");
        mealsTitle.setGraphic(Theme.icon("meal", 24));
        mealsTitle.setGraphicTextGap(10);
        VBox meals = card(mealsTitle, mealsBox, new HBox(10, find, shop));

        root.setPadding(new Insets(22, 26, 26, 26));
        featuredCard.setId("todayFeatured");
        featuredCard.getStyleClass().add("pnp-card");
        Theme.show(featuredCard, false);                        // appears when today's picks arrive
        root.getChildren().addAll(stats, featuredCard, meals);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        refresh();
    }

    public Parent getView() {
        return scroller;
    }

    /** What "Plan this meal" does: AppShell opens the meal on the Recipes page. */
    public void setOnPlanFeatured(Consumer<MealData> onPlanFeatured) {
        this.onPlanFeatured = onPlanFeatured;
    }

    /**
     * Shows today's picks: the first one large, the next three as small photo cards. The picks come
     * from the Recipes page, which already loaded them, so TheMealDB is not asked again. With no
     * picks (no internet) the card stays hidden.
     */
    public void showFeatured(List<MealData> picks, String pickTitle) {
        if (picks == null || picks.isEmpty()) {
            Theme.show(featuredCard, false);
            return;
        }
        MealData first = picks.get(0);
        Label name = Theme.text(first.name(), "pnp-featured-name");
        name.setWrapText(true);
        Label about = Theme.text("A real recipe from TheMealDB. See its ingredients and steps, then add it to your plan.",
                "pnp-muted");
        about.setWrapText(true);
        Button plan = new Button("Plan this meal");
        plan.setId("todayPlanFeatured");
        plan.getStyleClass().add("pnp-button");
        plan.setGraphic(Theme.icon("forkknife", 16));
        plan.setGraphicTextGap(8);
        plan.setOnAction(e -> onPlanFeatured.accept(first));
        VBox words = new VBox(8, Theme.text("FEATURED TODAY \u00B7 " + pickTitle.toUpperCase(Locale.ENGLISH), "pnp-overline"),
                name, about, plan);
        words.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(words, Priority.ALWAYS);
        HBox hero = new HBox(18, MealPhotos.photo(first.thumbnail(), first.name(), 300, 190), words);
        hero.setAlignment(Pos.CENTER_LEFT);
        StackPane heroWithMark = Theme.withWatermark(hero, "food", 190, 0.10, 16);   // faint botanical drawing behind

        HBox more = new HBox(12);
        picks.stream().skip(1).limit(3).forEach(meal -> more.getChildren().add(ideaCard(meal)));
        featuredCard.getChildren().setAll(heroWithMark);
        if (!more.getChildren().isEmpty()) {
            featuredCard.getChildren().addAll(Theme.text("MORE IDEAS", "pnp-stat-label"), more);
        }
        Theme.show(featuredCard, true);
    }

    /** A small clickable card: the meal's photo with its name under it. */
    private VBox ideaCard(MealData meal) {
        Label name = Theme.text(meal.name(), "pnp-card-title");
        name.setWrapText(true);
        VBox card = new VBox(8, MealPhotos.photo(meal.thumbnail(), meal.name(), 104), name);
        card.getStyleClass().addAll("pnp-card", "pnp-recipe-card");
        card.setPrefWidth(10);
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setOnMouseClicked(e -> onPlanFeatured.accept(meal));
        return card;
    }

    /** The streak card: the number and badge, the bar toward the next goal, and this week's days. */
    private VBox buildStreakCard() {
        streakValue.getStyleClass().add("pnp-streak-number");
        streakUnit.getStyleClass().add("pnp-muted");
        badgeChip.setId("todayBadge");
        badgeChip.getStyleClass().addAll("pnp-chip", "chip-tag");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = new HBox(8, Theme.icon("streak", 18), Theme.text("CURRENT STREAK", "pnp-stat-label"), gap, badgeChip);
        head.setAlignment(Pos.CENTER_LEFT);
        HBox number = new HBox(8, streakValue, streakUnit);
        number.setAlignment(Pos.BASELINE_LEFT);

        goalBar.setId("todayGoalBar");
        goalBar.getStyleClass().add("pnp-progress");
        goalBar.setMaxWidth(Double.MAX_VALUE);
        goalBar.setPrefHeight(10);
        goalLabel.setId("todayGoal");
        goalLabel.getStyleClass().add("pnp-muted");
        streakNote.getStyleClass().add("pnp-badge");
        streakNote.setWrapText(true);

        streakCard.getChildren().addAll(head, number, goalBar, goalLabel,
                Theme.text("THIS WEEK", "pnp-stat-label"), weekStrip,
                Theme.text("A rose circle means at least one meal is planned that day.", "pnp-muted"), streakNote);
        streakCard.getStyleClass().addAll("pnp-card", "pnp-streak-card");
        HBox.setHgrow(streakCard, Priority.ALWAYS);
        return streakCard;
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
        boolean atRisk = StreakCalculator.atRisk(data.days(), today);
        streakUnit.setText(current == 1 ? "day in a row" : "days in a row");
        badgeChip.setText(StreakCalculator.badgeTitle(tiers) + " badge");
        Theme.show(badgeChip, current > 0);
        goalBar.setProgress(StreakCalculator.milestoneProgress(current));
        goalLabel.setText(current == 0 ? "Plan a meal 3 days in a row to reach your first goal."
                : StreakCalculator.goalLine(current) + ".");
        streakCard.getStyleClass().remove("at-risk");
        if (atRisk) {
            streakCard.getStyleClass().add("at-risk");
        }
        streakNote.setText(atRisk ? "Nothing is planned today yet. Plan a meal today to keep your streak going!"
                : current == 0 ? "Plan a meal today to start a streak."
                : StreakCalculator.badgeTitle(tiers) + " badge: " + StreakCalculator.badgeReason(tiers)
                        + " (" + StreakCalculator.tierLine(tiers) + ").");
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
