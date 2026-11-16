package com.platenplatter.ui;

import com.platenplatter.controller.MealPlanController;
import com.platenplatter.controller.MealPlanController.DayPlan;
import com.platenplatter.controller.MealPlanController.DayTotals;
import com.platenplatter.controller.UserMessages;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.util.CalendarMath;
import com.platenplatter.util.CookingQueue;
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
import java.util.stream.Collectors;
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
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The Meal Plan page: a month calendar where each day is colored by the Healthy Level most of
 * its meals have (and marked H, M or I, so color is never the only cue), and the chosen day's
 * card. The day card shows the day's totals (worked out recursively), the cooking order (a
 * queue: "Done, next" takes the front meal off), and the meals. Remove takes a meal off at once
 * and an Undo bar appears; Undo puts back the last removed meal first (a stack).
 * Every handler calls one MealPlanController method.
 */
public class MealPlanScreen {
    private final MealPlanController controller;
    private final Consumer<LocalDate> onAddMealFor;
    private final Supplier<LocalDate> todayProvider;
    private Runnable onPlanChanged = () -> { };
    private YearMonth month;
    private LocalDate selected;
    private Map<LocalDate, HealthyLevel> dayLevels = Map.of();
    private CookingQueue cookingOrder = new CookingQueue();
    private final VBox root = new VBox(14);
    private final Label monthTitle = new Label();
    private final GridPane grid = new GridPane();
    private final Label dayTitle = new Label();
    private final Label dayTotals = new Label();
    private final VBox queueBox = new VBox(6);
    private final VBox dayMeals = new VBox(8);
    private final HBox undoBar = new HBox();
    private final Label undoText = new Label();
    private final Label status = new Label();

    public MealPlanScreen(MealPlanController controller, Consumer<LocalDate> onAddMealFor) {
        this(controller, onAddMealFor, LocalDate::now);
    }

    public MealPlanScreen(MealPlanController controller, Consumer<LocalDate> onAddMealFor,
                          Supplier<LocalDate> todayProvider) {
        this.controller = controller;
        this.onAddMealFor = onAddMealFor;
        this.todayProvider = todayProvider;
        this.selected = todayProvider.get();
        this.month = YearMonth.from(selected);
        VBox calendar = buildCalendarCard();
        calendar.setPrefWidth(470);
        calendar.setMinWidth(440);
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
                return controller.monthLevels(shown);
            }
        };
        task.setOnSucceeded(e -> {
            if (shown.equals(month)) {           // the user may have moved to another month already
                dayLevels = task.getValue();
                drawMonth();
                loadDay();
            }
        });
        task.setOnFailed(e -> showError(task.getException()));
        AppShell.runInBackground(task);
    }

    private VBox buildCalendarCard() {
        Button prev = navButton("<", "prevMonth", () -> goToMonth(month.minusMonths(1)));
        Button next = navButton(">", "nextMonth", () -> goToMonth(month.plusMonths(1)));
        Button today = navButton("Today", "goToday", () -> {
            selected = todayProvider.get();
            goToMonth(YearMonth.from(selected));
        });
        prev.setTooltip(new Tooltip("Previous month"));
        next.setTooltip(new Tooltip("Next month"));
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
            Label swatch = Theme.text(mark(level), "pnp-day-mark");
            swatch.setAlignment(Pos.CENTER);
            swatch.getStyleClass().addAll("pnp-legend-swatch", "legend-"
                    + level.name().toLowerCase());
            legend.getChildren().addAll(swatch, Theme.text(level.getLabel(), "pnp-legend-text"));
        });
        Region ring = new Region();
        ring.getStyleClass().add("pnp-legend-ring");
        legend.getChildren().addAll(ring, Theme.text("Today", "pnp-legend-text"));
        legend.setAlignment(Pos.CENTER_LEFT);
        Label legendNote = Theme.text("A day's color and letter come from most of its meals.",
                "pnp-hint");
        VBox card = new VBox(12, nav, grid, legend, legendNote);
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
        dayTotals.setId("dayTotals");
        dayTotals.getStyleClass().add("pnp-day-totals");
        dayTotals.setWrapText(true);
        queueBox.setId("cookingOrder");
        queueBox.getStyleClass().add("pnp-queue-box");
        undoText.getStyleClass().add("pnp-bold");
        HBox.setHgrow(undoText, Priority.ALWAYS);
        undoText.setMaxWidth(Double.MAX_VALUE);
        Button undo = new Button("Undo");
        undo.setId("undoRemove");
        undo.getStyleClass().add("pnp-button-secondary");
        undo.setOnAction(e -> undoRemove());
        undoBar.getChildren().addAll(undoText, undo);
        undoBar.getStyleClass().add("pnp-undo-bar");
        Theme.show(undoBar, false);
        ScrollPane scroll = new ScrollPane(dayMeals);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        Button add = new Button("Add a meal for this day");
        add.setId("addMealForDay");
        add.getStyleClass().add("pnp-button");
        add.setOnAction(e -> onAddMealFor.accept(selected));
        VBox card = new VBox(10, dayTitle, dayTotals, queueBox, undoBar, scroll, add);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void goToMonth(YearMonth target) {
        month = target;
        dayLevels = Map.of(); // draw the new month at once, then color it when its meals are read
        drawMonth();
        refresh();
    }

    private void drawMonth() {
        monthTitle.setText(month.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                .toUpperCase(Locale.ENGLISH) + " " + month.getYear());
        grid.getChildren().clear();
        Arrays.stream(DayOfWeek.values()).forEach(day -> {
            Label head = Theme.text(day.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                    "pnp-weekday-head");
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                head.getStyleClass().add("weekend");
            }
            GridPane.setHalignment(head, HPos.CENTER);
            grid.add(head, day.getValue() - 1, 0);
        });
        LocalDate today = todayProvider.get();
        IntStream.rangeClosed(1, month.lengthOfMonth()).mapToObj(month::atDay)
                .forEach(date -> grid.add(dayButton(date, today), CalendarMath.column(date),
                        CalendarMath.row(date) + 1));
    }

    /**
     * One day: its number, and under it the letter of its healthy level (and a tooltip in words).
     */
    private Button dayButton(LocalDate date, LocalDate today) {
        HealthyLevel level = dayLevels.get(date);
        Label number = Theme.text(String.valueOf(date.getDayOfMonth()), "pnp-day-number");
        Label letter = Theme.text(level == null ? " " : mark(level), "pnp-day-mark");
        VBox face = new VBox(0, number, letter);
        face.setAlignment(Pos.CENTER);
        face.setMouseTransparent(true);
        Button b = new Button();
        b.setGraphic(face);
        b.getStyleClass().add("pnp-day");
        b.setId("day-" + date);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setPrefHeight(48);
        String words = TextFormat.shortDate(date) + (date.equals(today) ? " (today)" : "")
                + (level == null ? ": nothing planned" : ": a " + level.getLabel() + " day");
        b.setTooltip(new Tooltip(words));
        b.setAccessibleText(words);
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
        return b;
    }

    /** H, M or I: the healthy level as a letter, so the calendar does not rely on color alone. */
    private static String mark(HealthyLevel level) {
        return level.getLabel().substring(0, 1);
    }

    private void loadDay() {
        LocalDate day = selected;
        dayTitle.setText("Meals on " + TextFormat.shortDate(day));
        Task<DayPlan> task = new Task<>() {
            @Override
            protected DayPlan call() throws Exception {
                return controller.day(day);
            }
        };
        task.setOnSucceeded(e -> {
            if (day.equals(selected)) {          // the user may have clicked another day already
                showDay(task.getValue());
            }
        });
        task.setOnFailed(e -> showError(task.getException()));
        AppShell.runInBackground(task);
    }

    private void showDay(DayPlan plan) {
        List<PlannedMeal> meals = plan.meals();
        dayTotals.setText(totalsText(plan.totals()));
        Theme.show(dayTotals, !meals.isEmpty());
        cookingOrder = plan.cookingOrder();
        drawQueue();
        dayMeals.getChildren().setAll(meals.isEmpty()
                ? List.<Node>of(Theme.emptyState("No meals planned.", "dishes"))
                : meals.stream().map(meal -> MealRows.row(meal, () -> remove(meal), true))
                        .toList());
    }

    /** "Day total: 3 meals · 1 h 20 min · 1,750 kcal (kcal by slot: Breakfast 450 · ...)" */
    private static String totalsText(DayTotals totals) {
        int meals = totals.slots().stream().mapToInt(MealPlanController.SlotTotal::meals).sum();
        String bySlot = totals.slots().stream()
                .map(slot -> slot.slot().getLabel() + " " + (slot.calories() > 0
                        ? String.format(Locale.ENGLISH, "%,d", slot.calories()) : "?"))
                .collect(Collectors.joining(" · "));
        return "Day total: " + meals + (meals == 1 ? " meal" : " meals") + " · "
                + TextFormat.minutes(totals.minutes()) + " · "
                + TextFormat.calories(totals.calories()) + "   (kcal by slot: " + bySlot + ")";
    }

    /**
     * The cooking order: the meal at the front of the queue, "Done, next", and what waits after.
     */
    private void drawQueue() {
        if (cookingOrder.isEmpty()) {
            queueBox.getChildren().setAll(Theme.text("COOKING ORDER", "pnp-overline"),
                    Theme.text("Nothing left to cook on this day.", "pnp-muted"));
            return;
        }
        SavedRecipe next = cookingOrder.peek();
        Button done = new Button("Done, next");
        done.setId("cookNext");
        done.getStyleClass().add("pnp-small-add");
        done.setOnAction(e -> {
            cookingOrder.next(); // first in, first out: the front meal leaves the queue
            drawQueue();
        });
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label upNext = Theme.text("Up next: " + next.getRecipeName(), "pnp-queue-next");
        HBox top = new HBox(8, upNext, gap, done);
        top.setAlignment(Pos.CENTER_LEFT);
        List<SavedRecipe> waiting = cookingOrder.inOrder();
        String after = waiting.size() <= 1 ? "Nothing after it."
                : "Then: " + waiting.stream().skip(1).map(SavedRecipe::getRecipeName)
                        .collect(Collectors.joining(", "));
        Label rest = Theme.text(after, "pnp-hint");
        rest.setWrapText(true);
        queueBox.getChildren().setAll(Theme.text("COOKING ORDER", "pnp-overline"), top, rest);
    }

    /** Removes at once, with no question: the Undo bar can bring the meal back. */
    private void remove(PlannedMeal meal) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                controller.remove(meal);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            showUndo();
            Theme.status(status, "", Theme.Kind.INFO);
            refresh();
            onPlanChanged.run();
        });
        task.setOnFailed(e -> showError(task.getException()));
        AppShell.runInBackground(task);
    }

    /** Puts back the most recently removed meal (the top of the undo stack). */
    private void undoRemove() {
        Task<PlannedMeal> task = new Task<>() {
            @Override
            protected PlannedMeal call() throws Exception {
                return controller.undoRemove();
            }
        };
        task.setOnSucceeded(e -> {
            PlannedMeal back = task.getValue();
            if (back != null) {
                Theme.status(status, "Put back \"" + back.getRecipeName() + "\" on "
                        + TextFormat.shortDate(back.getPlanDate()) + ".", Theme.Kind.OK);
            }
            showUndo();
            refresh();
            onPlanChanged.run();
        });
        task.setOnFailed(e -> showError(task.getException()));
        AppShell.runInBackground(task);
    }

    private void showUndo() {
        boolean can = controller.canUndo();
        undoText.setText(can ? "Removed \"" + controller.undoName() + "\"." : "");
        Theme.show(undoBar, can);
    }

    private void showError(Throwable error) {
        Theme.status(status, UserMessages.of(error), Theme.Kind.ERROR);
    }
}
