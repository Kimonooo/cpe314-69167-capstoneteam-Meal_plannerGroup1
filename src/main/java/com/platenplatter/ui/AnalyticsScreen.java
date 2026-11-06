package com.platenplatter.ui;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;
import com.platenplatter.util.StatFormat;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

/**
 * The "Analytics" tab (Weekly Statistics in the design document).
 * What it shows, all from the MINED result (database -> StatisticsService -> chart):
 *   - four number cards: meals, healthy share, average prep time, average calories;
 *   - a grouped bar chart: meals per day, split by Healthy level (green / yellow / red),
 *     Prep time (Quick / Average / Slow) or Meal slot (Breakfast / Lunch / Dinner / Snack);
 *   - a breakdown of the same meals by all three groupings.
 * "Week" shows one week (Monday to Sunday, with < and > to move); "All time" adds up every meal by
 * day of the week. Nothing is hard-coded: change a meal and the chart changes.
 * The work runs in a background Task. Controls that work: Week / All time, < >, This week,
 * Group bars by, Refresh.
 */
public class AnalyticsScreen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    /** Everything one refresh works out (made in the background, shown on the UI thread). */
    private static final class Snapshot {
        final boolean weekScope;
        final LocalDate monday;
        final Grouping grouping;
        final Map<String, Map<String, Integer>> table;
        final Map<String, Integer> byLevel;
        final Map<String, Integer> byTier;
        final Map<String, Integer> bySlot;
        final int total;
        final double healthyShare;
        final double averagePrep;
        final double averageCalories;
        final int withCalories;

        Snapshot(boolean weekScope, LocalDate monday, Grouping grouping, List<PlannedMeal> rows) {
            this.weekScope = weekScope;
            this.monday = monday;
            this.grouping = grouping;
            this.table = weekScope ? StatisticsService.countByDay(rows, monday, grouping)
                    : StatisticsService.countByWeekday(rows, grouping);
            this.byLevel = StatisticsService.totals(rows, Grouping.HEALTHY_LEVEL);
            this.byTier = StatisticsService.totals(rows, Grouping.PREP_TIME);
            this.bySlot = StatisticsService.totals(rows, Grouping.MEAL_SLOT);
            this.total = rows.size();
            this.healthyShare = StatisticsService.healthyShare(rows);
            this.averagePrep = StatisticsService.averagePrepMinutes(rows);
            this.averageCalories = StatisticsService.averageCalories(rows);
            int known = 0;
            for (PlannedMeal meal : rows) {
                if (meal.getCalories() > 0) {
                    known++;
                }
            }
            this.withCalories = known;
        }
    }

    private final PlanStore store;
    private final Supplier<LocalDate> todayProvider;
    private LocalDate monday;

    private final VBox root = new VBox(14);
    private final Button refreshButton = new Button("Refresh");
    private final Label updatedLabel = new Label();
    private final ToggleGroup scopeGroup = new ToggleGroup();
    private final ToggleButton weekToggle = new ToggleButton("Week");
    private final ToggleButton allToggle = new ToggleButton("All time");
    private final Button prevWeek = new Button("<");
    private final Button nextWeek = new Button(">");
    private final Button thisWeek = new Button("This week");
    private final Label weekLabel = new Label();
    private final HBox weekNav = new HBox(6);
    private final ComboBox<Grouping> groupingBox = new ComboBox<>();

    private final Label totalCaption = new Label("MEALS THIS WEEK");
    private final Label totalValue = new Label("0");
    private final Label healthyValue = new Label("-");
    private final Label healthyUnit = new Label("of meals are Healthy");
    private final Label avgValue = new Label("-");
    private final Label caloriesValue = new Label("-");
    private final Label caloriesUnit = new Label("per meal");
    private final StackPane chartHolder = new StackPane();
    private final VBox breakdownRows = new VBox(14);
    private final Label emptyLabel = new Label();
    private final Label statusLabel = new Label();

    public AnalyticsScreen(PlanStore store) {
        this(store, LocalDate::now);
    }

    /** The second constructor lets a test pretend it is any day. */
    public AnalyticsScreen(PlanStore store, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.todayProvider = todayProvider;
        this.monday = StatisticsService.mondayOf(todayProvider.get());

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildControls(), buildStatRow(), buildMainArea(), statusLabel);
        statusLabel.setId("analyticsStatus");
        Theme.apply(root);
        wireEvents();
        refresh();
    }

    public Parent getView() {
        return root;
    }

    /** Re-reads the data, re-runs the mining step and redraws the chart (off the UI thread). */
    public void refresh() {
        boolean weekScope = weekToggle.isSelected();
        LocalDate shownMonday = monday;
        Grouping grouping = groupingBox.getValue() == null ? Grouping.HEALTHY_LEVEL : groupingBox.getValue();
        weekLabel.setText("Week of " + shownMonday.format(WEEK));
        refreshButton.setDisable(true);
        Theme.status(statusLabel, "Loading...", Theme.Kind.INFO);

        Task<Snapshot> task = new Task<>() {
            @Override
            protected Snapshot call() throws Exception {
                List<PlannedMeal> rows = weekScope
                        ? store.mealsBetween(shownMonday, shownMonday.plusDays(6))
                        : store.allMeals();
                return new Snapshot(weekScope, shownMonday, grouping, rows);
            }
        };
        task.setOnSucceeded(e -> show(task.getValue()));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Theme.status(statusLabel, "Could not load data: " + (ex == null ? "unknown error" : ex.getMessage()),
                    Theme.Kind.ERROR);
            refreshButton.setDisable(false);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ------------------------------------------------------------------ building the screen

    private HBox buildHeader() {
        Label title = new Label("Meal Plan Analytics");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("How healthy, how quick, and which meals? Counted straight from your meal plan.");
        subtitle.getStyleClass().add("pnp-subtitle");
        VBox texts = new VBox(2, title, subtitle);

        refreshButton.setId("refreshButton");
        refreshButton.getStyleClass().add("pnp-button");
        updatedLabel.getStyleClass().add("pnp-muted");
        updatedLabel.setId("updatedLabel");
        VBox right = new VBox(4, refreshButton, updatedLabel);
        right.setAlignment(Pos.CENTER_RIGHT);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox header = new HBox(texts, spacer, right);
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    private HBox buildControls() {
        weekToggle.setId("scopeWeek");
        allToggle.setId("scopeAll");
        weekToggle.getStyleClass().add("pnp-toggle");
        allToggle.getStyleClass().add("pnp-toggle");
        weekToggle.setToggleGroup(scopeGroup);
        allToggle.setToggleGroup(scopeGroup);
        weekToggle.setSelected(true);

        prevWeek.setId("prevWeek");
        nextWeek.setId("nextWeek");
        thisWeek.setId("thisWeek");
        prevWeek.getStyleClass().add("pnp-nav-button");
        nextWeek.getStyleClass().add("pnp-nav-button");
        thisWeek.getStyleClass().add("pnp-nav-button");
        weekLabel.setId("weekLabel");
        weekLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C0008;");
        weekLabel.setMinWidth(150);
        weekLabel.setAlignment(Pos.CENTER);
        weekNav.getChildren().addAll(prevWeek, weekLabel, nextWeek, thisWeek);
        weekNav.setAlignment(Pos.CENTER_LEFT);

        groupingBox.setId("groupingBox");
        groupingBox.getItems().addAll(Grouping.values());
        groupingBox.setValue(Grouping.HEALTHY_LEVEL);
        Label groupLabel = new Label("Group bars by");
        groupLabel.getStyleClass().add("pnp-muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, weekToggle, allToggle, weekNav, spacer, groupLabel, groupingBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private HBox buildStatRow() {
        totalValue.setId("totalValue");
        healthyValue.setId("healthyValue");
        avgValue.setId("avgValue");
        caloriesValue.setId("caloriesValue");
        return new HBox(14,
                statCard(totalCaption, totalValue, new Label("planned")),
                statCard(new Label("HEALTHY SHARE"), healthyValue, healthyUnit),
                statCard(new Label("AVERAGE PREP TIME"), avgValue, new Label("per meal")),
                statCard(new Label("AVERAGE CALORIES"), caloriesValue, caloriesUnit));
    }

    private VBox statCard(Label caption, Label valueLabel, Label unit) {
        valueLabel.getStyleClass().add("pnp-stat-value");
        caption.getStyleClass().add("pnp-stat-label");
        unit.getStyleClass().add("pnp-muted");
        VBox card = new VBox(2, caption, valueLabel, unit);
        card.getStyleClass().add("pnp-card");
        card.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.setPrefWidth(10);
        return card;
    }

    private HBox buildMainArea() {
        emptyLabel.getStyleClass().add("pnp-empty");
        emptyLabel.setWrapText(true);
        emptyLabel.setStyle("-fx-text-alignment: center;");
        chartHolder.setMinHeight(300);
        chartHolder.getChildren().add(emptyLabel);
        VBox chartCard = new VBox(chartHolder);
        chartCard.getStyleClass().add("pnp-card");
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        VBox.setVgrow(chartHolder, Priority.ALWAYS);

        Label breakdownTitle = new Label("Breakdown");
        breakdownTitle.getStyleClass().add("pnp-section");
        ScrollPane scroll = new ScrollPane(breakdownRows);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        VBox breakdownCard = new VBox(12, breakdownTitle, scroll);
        breakdownCard.getStyleClass().add("pnp-card");
        breakdownCard.setPrefWidth(260);
        breakdownCard.setMinWidth(230);
        breakdownCard.setMaxWidth(290);

        HBox main = new HBox(14, chartCard, breakdownCard);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private void wireEvents() {
        refreshButton.setOnAction(e -> refresh());
        scopeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true); // one of the two is always selected
                return;
            }
            boolean week = newToggle == weekToggle;
            weekNav.setDisable(!week);
            totalCaption.setText(week ? "MEALS THIS WEEK" : "MEALS (ALL TIME)");
            refresh();
        });
        prevWeek.setOnAction(e -> {
            monday = monday.minusWeeks(1);
            refresh();
        });
        nextWeek.setOnAction(e -> {
            monday = monday.plusWeeks(1);
            refresh();
        });
        thisWeek.setOnAction(e -> {
            monday = StatisticsService.mondayOf(todayProvider.get());
            refresh();
        });
        groupingBox.valueProperty().addListener((obs, oldValue, newValue) -> refresh());
    }

    // ------------------------------------------------------------------ showing the result

    private void show(Snapshot snap) {
        boolean isThisWeek = snap.monday.equals(StatisticsService.mondayOf(todayProvider.get()));
        totalCaption.setText(snap.weekScope ? (isThisWeek ? "MEALS THIS WEEK" : "MEALS THAT WEEK") : "MEALS (ALL TIME)");
        totalValue.setText(String.format("%,d", snap.total));
        healthyValue.setText(Double.isNaN(snap.healthyShare) ? "-" : String.format("%.0f%%", snap.healthyShare * 100));
        avgValue.setText(StatFormat.average(snap.averagePrep));
        if (snap.withCalories == 0) {
            caloriesValue.setText("-");
            caloriesUnit.setText("no calories given yet");
        } else {
            caloriesValue.setText(String.format("%,.0f kcal", snap.averageCalories));
            caloriesUnit.setText(String.format("per meal (%,d with calories)", snap.withCalories));
        }

        breakdownRows.getChildren().clear();
        if (snap.total == 0) {
            emptyLabel.setText(snap.weekScope
                    ? "No meals planned in the week of " + snap.monday.format(WEEK) + ".\nAdd a meal from the Recipes tab, or try another week."
                    : "No meal data yet.\nImport a CSV or add a meal from the Recipes tab.");
            chartHolder.getChildren().setAll(emptyLabel);
            Label none = new Label("Nothing to count yet.");
            none.getStyleClass().add("pnp-muted");
            breakdownRows.getChildren().add(none);
        } else {
            String title = "Meals Planned per Day by " + titleCase(snap.grouping.getLabel())
                    + (snap.weekScope ? ", Week of " + snap.monday.format(WEEK) : ", All Time");
            BarChart<String, Number> chart = AnalyticsChart.buildGrouped(title, snap.table, snap.grouping);
            chartHolder.getChildren().setAll(chart);
            breakdownRows.getChildren().addAll(
                    breakdownSection("By healthy level", snap.byLevel, snap.total, levelStyles()),
                    breakdownSection("By prep time", snap.byTier, snap.total, tierStyles()),
                    breakdownSection("By meal slot", snap.bySlot, snap.total, slotStyles()));
        }
        updatedLabel.setText("Updated " + LocalTime.now().format(CLOCK));
        Theme.status(statusLabel, snap.total == 0 ? "No data yet." : "Chart is up to date.",
                snap.total == 0 ? Theme.Kind.INFO : Theme.Kind.OK);
        refreshButton.setDisable(false);
    }

    /** One group of breakdown rows, for example "By prep time": Quick 12 (40%), Average ..., Slow ... */
    private VBox breakdownSection(String heading, Map<String, Integer> counts, int total, String[] barStyles) {
        Label title = new Label(heading);
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #5C0008;");
        VBox section = new VBox(8, title);
        int i = 0;
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            String style = i < barStyles.length ? barStyles[i] : "";
            section.getChildren().add(breakdownRow(entry.getKey(), entry.getValue(), total, style));
            i++;
        }
        return section;
    }

    private VBox breakdownRow(String name, int count, int total, String barStyle) {
        double share = total == 0 ? 0 : (double) count / total;
        Label label = new Label(name);
        label.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label numbers = new Label(String.format("%,d  (%.0f%%)", count, share * 100));
        numbers.getStyleClass().add("pnp-muted");
        HBox top = new HBox(label, spacer, numbers);

        ProgressBar bar = new ProgressBar(share);
        bar.getStyleClass().add("pnp-mini-bar");
        if (!barStyle.isEmpty()) {
            bar.getStyleClass().add(barStyle);
        }
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, top, bar);
    }

    /** The mini-bar colors, in the same order as the groups (and the same colors as the chart). */
    private static String[] levelStyles() {
        String[] out = new String[HealthyLevel.values().length];
        for (HealthyLevel level : HealthyLevel.values()) {
            out[level.ordinal()] = "bar-" + level.name().toLowerCase();
        }
        return out;
    }

    private static String[] tierStyles() {
        String[] out = new String[PrepTier.values().length];
        for (PrepTier tier : PrepTier.values()) {
            out[tier.ordinal()] = "bar-" + tier.name().toLowerCase();
        }
        return out;
    }

    private static String[] slotStyles() {
        String[] out = new String[MealSlot.values().length];
        for (MealSlot slot : MealSlot.values()) {
            out[slot.ordinal()] = "bar-" + slot.name().toLowerCase();
        }
        return out;
    }

    /** "Healthy level" becomes "Healthy Level" (for the chart title). */
    private static String titleCase(String text) {
        StringBuilder out = new StringBuilder();
        for (String word : text.split(" ")) {
            if (out.length() > 0) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }
}
