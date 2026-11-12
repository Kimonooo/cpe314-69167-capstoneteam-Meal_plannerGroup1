package com.platenplatter.ui;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.CalendarMath;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.TextFormat;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import javafx.concurrent.Task;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The Meal Plan page: a month calendar where each day is colored by the Healthy Level most of
 * its meals have, and the meals of the chosen day, which can be removed.
 */
public class MealPlanScreen {
    private final PlanStore store;
    private final Consumer<LocalDate> onAddMealFor;
    private final Supplier<LocalDate> todayProvider;
    private Runnable onPlanChanged = () -> { };
    private YearMonth month;
    private LocalDate selected;
    private Map<LocalDate, HealthyLevel> dayLevels = Map.of();
    private final VBox root = new VBox(14);
    private final Label monthTitle = new Label();
    private final GridPane grid = new GridPane();
    private final Label dayTitle = new Label();
    private final VBox dayMeals = new VBox(8);
    private final Label status = new Label();

    public MealPlanScreen(PlanStore store, Consumer<LocalDate> onAddMealFor) {
        this(store, onAddMealFor, LocalDate::now);
    }

    public MealPlanScreen(PlanStore store, Consumer<LocalDate> onAddMealFor, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.onAddMealFor = onAddMealFor;
        this.todayProvider = todayProvider;
        this.selected = todayProvider.get();
        this.month = YearMonth.from(selected);
        VBox calendar = buildCalendarCard();
        calendar.setPrefWidth(460);
        calendar.setMinWidth(430);
        VBox day = buildDayCard();
        HBox.setHgrow(day, Priority.ALWAYS);
        HBox main = new HBox(16, calendar, day);
        VBox.setVgrow(main, Priority.ALWAYS);
        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(main, status);
        status.setId("planStatus");
        Theme.apply(root);
        refresh();
    }

    public Parent getView() {
        return root;
    }

    public void setOnPlanChanged(Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged;
    }

    /** Reads the month's meals in the background, colors the days, then shows the chosen day. */
    public void refresh() {
        YearMonth shown = month;
        Task<Map<LocalDate, HealthyLevel>> task = new Task<>() {
            @Override
            protected Map<LocalDate, HealthyLevel> call() throws Exception {
                return StatisticsService.levelByDay(store.mealsBetween(shown.atDay(1), shown.atEndOfMonth()));
            }
        };
        task.setOnSucceeded(e -> {
            if (shown.equals(month)) {           // the user may have moved to another month already
                dayLevels = task.getValue();
                drawMonth();
                loadDay();
            }
        });
        task.setOnFailed(e -> Theme.status(status, "Could not read the meal plan.", Theme.Kind.ERROR));
        AppShell.runInBackground(task);
    }

    private VBox buildCalendarCard() {
        Button prev = navButton("<", "prevMonth", () -> goToMonth(month.minusMonths(1)));
        Button next = navButton(">", "nextMonth", () -> goToMonth(month.plusMonths(1)));
        Button today = navButton("Today", "goToday", () -> {
            selected = todayProvider.get();
            goToMonth(YearMonth.from(selected));
        });
        monthTitle.getStyleClass().add("pnp-month-title");
        monthTitle.setId("monthTitle");
        monthTitle.setMaxWidth(Double.MAX_VALUE);
        monthTitle.setAlignment(Pos.CENTER);
        HBox.setHgrow(monthTitle, Priority.ALWAYS);
        HBox nav = new HBox(8, prev, monthTitle, today, next);
        nav.setAlignment(Pos.CENTER);
        grid.setHgap(6);
        grid.setVgap(6);
        IntStream.range(0, 7).forEach(c -> {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 7);
            grid.getColumnConstraints().add(column);
        });
        HBox legend = new HBox(8);
        Arrays.stream(HealthyLevel.values()).forEach(level -> {
            Region swatch = new Region();
            swatch.getStyleClass().addAll("pnp-legend-swatch", "legend-" + level.name().toLowerCase());
            legend.getChildren().addAll(swatch, Theme.text(level.getLabel(), "pnp-muted"));
        });
        Region ring = new Region();
        ring.getStyleClass().add("pnp-legend-ring");
        legend.getChildren().addAll(ring, Theme.text("today", "pnp-muted"));
        legend.setAlignment(Pos.CENTER_LEFT);
        VBox card = new VBox(12, nav, grid, legend);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private static Button navButton(String text, String id, Runnable action) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("pnp-nav-button");
        button.setOnAction(e -> action.run());
        return button;
    }

    private VBox buildDayCard() {
        dayTitle.getStyleClass().add("pnp-section");
        dayTitle.setGraphic(Theme.icon("meal", 24));
        dayTitle.setGraphicTextGap(10);
        dayTitle.setId("dayTitle");
        ScrollPane scroll = new ScrollPane(dayMeals);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Button add = new Button("Add a meal for this day");
        add.setId("addMealForDay");
        add.getStyleClass().add("pnp-button");
        add.setOnAction(e -> onAddMealFor.accept(selected));
        VBox card = new VBox(10, dayTitle, scroll, add);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void goToMonth(YearMonth target) {
        month = target;
        dayLevels = Map.of();       // draw the new month at once, then color it when its meals are read
        drawMonth();
        refresh();
    }

    private void drawMonth() {
        monthTitle.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase() + " " + month.getYear());
        grid.getChildren().clear();
        Arrays.stream(DayOfWeek.values()).forEach(day -> {
            Label head = Theme.text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH), "pnp-weekday-head");
            GridPane.setHalignment(head, HPos.CENTER);
            grid.add(head, day.getValue() - 1, 0);
        });
        LocalDate today = todayProvider.get();
        IntStream.rangeClosed(1, month.lengthOfMonth()).mapToObj(month::atDay).forEach(date -> {
            Button b = new Button(String.valueOf(date.getDayOfMonth()));
            b.getStyleClass().add("pnp-day");
            b.setId("day-" + date);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(42);
            HealthyLevel level = dayLevels.get(date);
            if (level != null) {
                b.getStyleClass().add("day-" + level.name().toLowerCase());
            }
            if (date.equals(today)) {
                b.getStyleClass().add("day-today");
            }
            if (date.equals(selected)) {
                b.getStyleClass().add("day-selected");
            }
            b.setOnAction(e -> {
                selected = date;
                drawMonth();
                loadDay();
            });
            grid.add(b, CalendarMath.column(date), CalendarMath.row(date) + 1);
        });
    }

    private void loadDay() {
        LocalDate day = selected;
        dayTitle.setText("Meals on " + TextFormat.shortDate(day));
        Task<List<PlannedMeal>> task = new Task<>() {
            @Override
            protected List<PlannedMeal> call() throws Exception {
                return store.mealsOn(day);
            }
        };
        task.setOnSucceeded(e -> {
            if (day.equals(selected)) {          // the user may have clicked another day already
                List<PlannedMeal> meals = task.getValue();
                dayMeals.getChildren().setAll(meals.isEmpty()
                        ? List.<Node>of(Theme.emptyState("No meals planned.", "dishes"))
                        : meals.stream().map(meal -> MealRows.row(meal, () -> remove(meal), true)).toList());
            }
        });
        task.setOnFailed(e -> Theme.status(status, "Could not read the meals of this day.", Theme.Kind.ERROR));
        AppShell.runInBackground(task);
    }

    private void remove(PlannedMeal meal) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                store.deleteMeal(meal.getPlannedMealId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Theme.status(status, "Removed \"" + meal.getRecipeName() + "\" from " + TextFormat.shortDate(meal.getPlanDate()) + ".",
                    Theme.Kind.OK);
            refresh();
            onPlanChanged.run();
        });
        task.setOnFailed(e -> Theme.status(status, "Could not remove the meal.", Theme.Kind.ERROR));
        AppShell.runInBackground(task);
    }
}
