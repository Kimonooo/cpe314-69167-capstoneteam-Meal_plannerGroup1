package com.platenplatter.ui;

import com.platenplatter.util.CalendarMath;
import com.platenplatter.util.DateTimeText;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
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

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The "Meal Plan" tab: a month calendar in the same cream and gold style as our first version.
 * Gold days have meals planned, the maroon outline is today, and the dark day is the one you picked.
 * Click a day to see its meals on the right, remove a meal, or jump to the Recipes tab to add one.
 * Controls that work: previous / next month, Today, every day button, Remove, Add a meal.
 * All database reads and deletes run in background Tasks.
 */
public class MealPlanScreen {
    private final PlanStore store;
    private final Consumer<LocalDate> onAddMealFor;
    private final Supplier<LocalDate> todayProvider;
    private Runnable onPlanChanged = () -> { };

    private YearMonth month;
    private LocalDate selected;
    private Set<LocalDate> plannedDates = new HashSet<>();

    private final VBox root = new VBox(14);
    private final Label monthTitle = new Label();
    private final GridPane grid = new GridPane();
    private final Label dayTitle = new Label();
    private final VBox dayMeals = new VBox(8);
    private final Label status = new Label();
    private final Button addButton = new Button("Add a meal for this day");

    public MealPlanScreen(PlanStore store, Consumer<LocalDate> onAddMealFor) {
        this(store, onAddMealFor, LocalDate::now);
    }

    public MealPlanScreen(PlanStore store, Consumer<LocalDate> onAddMealFor, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.onAddMealFor = onAddMealFor;
        this.todayProvider = todayProvider;
        this.selected = todayProvider.get();
        this.month = YearMonth.from(selected);

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildMain(), status);
        Theme.apply(root);
        status.setId("planStatus");
        refresh();
    }

    public Parent getView() {
        return root;
    }

    public void setOnPlanChanged(Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged == null ? () -> { } : onPlanChanged;
    }

    /** Re-reads which days have meals and the meals of the picked day. */
    public void refresh() {
        Task<Set<LocalDate>> task = new Task<>() {
            @Override
            protected Set<LocalDate> call() throws Exception {
                return store.plannedDates();
            }
        };
        task.setOnSucceeded(e -> {
            plannedDates = task.getValue();
            drawMonth();
            loadDay();
        });
        task.setOnFailed(e -> Theme.status(status, "Could not read the meal plan.", Theme.Kind.ERROR));
        runInBackground(task);
    }

    // ------------------------------------------------------------------ building the screen

    private VBox buildHeader() {
        Label title = new Label("Meal Plan");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("Pick a day to see what is planned. Gold days have meals.");
        subtitle.getStyleClass().add("pnp-subtitle");
        return new VBox(2, title, subtitle);
    }

    private HBox buildMain() {
        VBox calendarCard = buildCalendarCard();
        calendarCard.setMinWidth(430);
        calendarCard.setPrefWidth(450);
        calendarCard.setMaxWidth(450);

        VBox dayCard = buildDayCard();
        HBox.setHgrow(dayCard, Priority.ALWAYS);
        HBox main = new HBox(16, calendarCard, dayCard);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private VBox buildCalendarCard() {
        Button prev = new Button("<");
        prev.setId("prevMonth");
        prev.getStyleClass().add("pnp-nav-button");
        prev.setOnAction(e -> goToMonth(month.minusMonths(1)));
        Button next = new Button(">");
        next.setId("nextMonth");
        next.getStyleClass().add("pnp-nav-button");
        next.setOnAction(e -> goToMonth(month.plusMonths(1)));
        Button todayButton = new Button("Today");
        todayButton.setId("goToday");
        todayButton.getStyleClass().add("pnp-nav-button");
        todayButton.setOnAction(e -> {
            selected = todayProvider.get();
            goToMonth(YearMonth.from(selected));
        });

        monthTitle.getStyleClass().add("pnp-month-title");
        monthTitle.setId("monthTitle");
        monthTitle.setMaxWidth(Double.MAX_VALUE);
        monthTitle.setAlignment(Pos.CENTER);
        HBox.setHgrow(monthTitle, Priority.ALWAYS);
        HBox nav = new HBox(8, prev, monthTitle, todayButton, next);
        nav.setAlignment(Pos.CENTER);

        grid.setHgap(6);
        grid.setVgap(6);
        for (int c = 0; c < 7; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            grid.getColumnConstraints().add(cc);
        }

        Region gold = new Region();
        gold.getStyleClass().add("pnp-legend-gold");
        Label goldText = new Label("meals planned");
        goldText.getStyleClass().add("pnp-muted");
        Region ring = new Region();
        ring.getStyleClass().add("pnp-legend-ring");
        Label ringText = new Label("today");
        ringText.getStyleClass().add("pnp-muted");
        HBox legend = new HBox(8, gold, goldText, ring, ringText);
        legend.setAlignment(Pos.CENTER_LEFT);

        VBox card = new VBox(12, nav, grid, legend);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildDayCard() {
        dayTitle.getStyleClass().add("pnp-section");
        dayTitle.setId("dayTitle");
        ScrollPane scroll = new ScrollPane(dayMeals);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        addButton.setId("addMealForDay");
        addButton.getStyleClass().add("pnp-button");
        addButton.setOnAction(e -> onAddMealFor.accept(selected));

        VBox card = new VBox(10, dayTitle, scroll, addButton);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    // ------------------------------------------------------------------ drawing

    private void goToMonth(YearMonth target) {
        month = target;
        drawMonth();
    }

    private void drawMonth() {
        monthTitle.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH).toUpperCase() + " " + month.getYear());
        grid.getChildren().clear();
        String[] heads = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int c = 0; c < 7; c++) {
            Label head = new Label(heads[c]);
            head.getStyleClass().add("pnp-weekday-head");
            head.setMaxWidth(Double.MAX_VALUE);
            GridPane.setHalignment(head, javafx.geometry.HPos.CENTER);
            grid.add(head, c, 0);
        }
        LocalDate today = todayProvider.get();
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            Button b = new Button(String.valueOf(day));
            b.getStyleClass().add("pnp-day");
            b.setId("day-" + date);
            b.setMaxWidth(Double.MAX_VALUE);
            b.setPrefHeight(42);
            if (plannedDates.contains(date)) {
                b.getStyleClass().add("day-has-meals");
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
        }
    }

    private void loadDay() {
        dayTitle.setText("Meals on " + DateTimeText.shortDate(selected));
        LocalDate day = selected;
        Task<List<PlannedMeal>> task = new Task<>() {
            @Override
            protected List<PlannedMeal> call() throws Exception {
                return store.mealsOn(day);
            }
        };
        task.setOnSucceeded(e -> {
            if (!day.equals(selected)) {
                return; // the user already clicked another day
            }
            showMeals(task.getValue());
        });
        task.setOnFailed(e -> Theme.status(status, "Could not read the meals of this day.", Theme.Kind.ERROR));
        runInBackground(task);
    }

    private void showMeals(List<PlannedMeal> meals) {
        dayMeals.getChildren().clear();
        if (meals.isEmpty()) {
            Label none = new Label("No meals planned.");
            none.getStyleClass().add("pnp-empty");
            dayMeals.getChildren().add(none);
            return;
        }
        for (PlannedMeal meal : meals) {
            dayMeals.getChildren().add(MealRows.row(meal, () -> remove(meal)));
        }
    }

    private void remove(PlannedMeal meal) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                store.deleteMeal(meal.getId());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Theme.status(status, "Removed \"" + meal.getRecipeName() + "\" from " + DateTimeText.shortDate(selected) + ".", Theme.Kind.OK);
            refresh();
            onPlanChanged.run();
        });
        task.setOnFailed(e -> Theme.status(status, "Could not remove the meal.", Theme.Kind.ERROR));
        runInBackground(task);
    }

    private static void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
