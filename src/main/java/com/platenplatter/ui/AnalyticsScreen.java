package com.platenplatter.ui;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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

/**
 * The Analytics page: the user's real meals are read in the background, mined by StatisticsService,
 * and drawn as a grouped bar chart with four summary cards and a breakdown panel.
 */
public class AnalyticsScreen {
    private static final DateTimeFormatter WEEK = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH);

    /** Everything the page shows, worked out on the background thread. */
    private record Snapshot(boolean weekScope, LocalDate monday, Grouping grouping, Map<String, Map<String, Long>> table,
                            List<PlannedMeal> meals) { }

    private final PlanStore store;
    private final Supplier<LocalDate> todayProvider;
    private LocalDate monday;
    private final VBox root = new VBox(14);
    private final Button refreshButton = new Button("Refresh");
    private final Label updatedLabel = new Label();
    private final ToggleGroup scopeGroup = new ToggleGroup();
    private final ToggleButton weekToggle = new ToggleButton("Week");
    private final ToggleButton allToggle = new ToggleButton("All time");
    private final Label weekLabel = new Label();
    private final HBox weekNav = new HBox(6);
    private final ComboBox<Grouping> groupingBox = new ComboBox<>();
    private final Label totalValue = new Label("0");
    private final Label totalUnit = new Label("planned this week");
    private final Label healthyValue = new Label("-");
    private final Label avgValue = new Label("-");
    private final Label caloriesValue = new Label("-");
    private final Label caloriesUnit = new Label("per meal");
    private final StackPane chartHolder = new StackPane();
    private final VBox breakdownRows = new VBox(14);
    private final Label statusLabel = new Label();

    public AnalyticsScreen(PlanStore store) {
        this(store, LocalDate::now);
    }

    public AnalyticsScreen(PlanStore store, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.todayProvider = todayProvider;
        this.monday = StatisticsService.mondayOf(todayProvider.get());
        refreshButton.setId("refreshButton");
        refreshButton.getStyleClass().add("pnp-button");
        refreshButton.setOnAction(e -> refresh());
        updatedLabel.getStyleClass().add("pnp-muted");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        VBox right = new VBox(4, refreshButton, updatedLabel);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(Theme.pageHeader("Meal Plan Analytics",
                "How healthy, how quick, and which meals? Counted straight from your meal plan."), gap, right);
        totalValue.setId("totalValue");
        healthyValue.setId("healthyValue");
        avgValue.setId("avgValue");
        caloriesValue.setId("caloriesValue");
        HBox stats = new HBox(14, Theme.statCard("MEALS", totalValue, totalUnit),
                Theme.statCard("HEALTHY SHARE", healthyValue, new Label("of meals are Healthy")),
                Theme.statCard("AVERAGE PREP TIME", avgValue, new Label("per meal")),
                Theme.statCard("AVERAGE CALORIES", caloriesValue, caloriesUnit));
        statusLabel.setId("analyticsStatus");
        VBox content = new VBox(14, header, buildControls(), stats, buildMainArea(), statusLabel);
        content.setPadding(new Insets(22, 26, 26, 26));
        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        root.getChildren().add(scroll);
        Theme.apply(root);
        refresh();
    }

    public Parent getView() {
        return root;
    }

    /** Reads and mines the meals in the background, then draws them. */
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
                List<PlannedMeal> meals = weekScope ? store.mealsBetween(shownMonday, shownMonday.plusDays(6)) : store.allMeals();
                return new Snapshot(weekScope, shownMonday, grouping, weekScope
                        ? StatisticsService.countByDay(meals, shownMonday, grouping)
                        : StatisticsService.countByWeekday(meals, grouping), meals);
            }
        };
        task.setOnSucceeded(e -> show(task.getValue()));
        task.setOnFailed(e -> {
            Theme.status(statusLabel, "Could not load data: " + task.getException().getMessage(), Theme.Kind.ERROR);
            refreshButton.setDisable(false);
        });
        AppShell.runInBackground(task);
    }

    private HBox buildControls() {
        weekToggle.setId("scopeWeek");
        allToggle.setId("scopeAll");
        List.of(weekToggle, allToggle).forEach(toggle -> {
            toggle.getStyleClass().add("pnp-toggle");
            toggle.setToggleGroup(scopeGroup);
        });
        weekToggle.setSelected(true);
        scopeGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle == null) {
                oldToggle.setSelected(true);         // one of the two is always selected
                return;
            }
            weekNav.setDisable(newToggle != weekToggle);
            refresh();
        });
        Button prev = weekButton("<", "prevWeek", () -> monday = monday.minusWeeks(1));
        Button next = weekButton(">", "nextWeek", () -> monday = monday.plusWeeks(1));
        Button thisWeek = weekButton("This week", "thisWeek", () -> monday = StatisticsService.mondayOf(todayProvider.get()));
        weekLabel.setId("weekLabel");
        weekLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #4E0920;");
        weekLabel.setMinWidth(150);
        weekLabel.setAlignment(Pos.CENTER);
        weekNav.getChildren().addAll(prev, weekLabel, next, thisWeek);
        weekNav.setAlignment(Pos.CENTER_LEFT);
        groupingBox.setId("groupingBox");
        groupingBox.getItems().addAll(Grouping.values());
        groupingBox.setValue(Grouping.HEALTHY_LEVEL);
        groupingBox.valueProperty().addListener((obs, old, now) -> refresh());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox row = new HBox(10, weekToggle, allToggle, weekNav, gap, Theme.text("Group bars by", "pnp-muted"), groupingBox);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private Button weekButton(String text, String id, Runnable move) {
        Button button = new Button(text);
        button.setId(id);
        button.getStyleClass().add("pnp-nav-button");
        button.setOnAction(e -> {
            move.run();
            refresh();
        });
        return button;
    }

    private HBox buildMainArea() {
        chartHolder.setMinHeight(320);
        VBox chartCard = new VBox(chartHolder);
        chartCard.getStyleClass().add("pnp-card");
        HBox.setHgrow(chartCard, Priority.ALWAYS);
        VBox breakdownCard = new VBox(12, Theme.text("Breakdown", "pnp-section"), breakdownRows);
        breakdownCard.getStyleClass().add("pnp-card");
        breakdownCard.setPrefWidth(260);
        breakdownCard.setMinWidth(230);
        return new HBox(14, chartCard, breakdownCard);
    }

    private void show(Snapshot snap) {
        List<PlannedMeal> meals = snap.meals();
        totalValue.setText(String.format(Locale.ENGLISH, "%,d", meals.size()));
        totalUnit.setText(snap.weekScope() ? "planned that week" : "planned (all time)");
        healthyValue.setText(TextFormat.percent(StatisticsService.healthyShare(meals)));
        avgValue.setText(TextFormat.averageMinutes(StatisticsService.averagePrepMinutes(meals)));
        long withCalories = StatisticsService.withCalories(meals);
        caloriesValue.setText(withCalories == 0 ? "-" : TextFormat.calories((int) Math.round(StatisticsService.averageCalories(meals))));
        caloriesUnit.setText(withCalories == 0 ? "no calories given yet" : "per meal (" + withCalories + " with calories)");
        if (meals.isEmpty()) {
            Label empty = Theme.text(snap.weekScope()
                    ? "No meals planned in the week of " + snap.monday().format(WEEK) + ".\nAdd a meal on Recipes, or try another week."
                    : "No meal data yet.\nImport a CSV or add a meal on Recipes.", "pnp-empty");
            chartHolder.getChildren().setAll(empty);
            breakdownRows.getChildren().setAll(Theme.text("Nothing to count yet.", "pnp-muted"));
        } else {
            String title = "Meals Planned per Day by " + titleCase(snap.grouping().getLabel())
                    + (snap.weekScope() ? ", Week of " + snap.monday().format(WEEK) : ", All Time");
            BarChart<String, Number> chart = AnalyticsChart.buildGrouped(title, snap.table(), snap.grouping());
            chartHolder.getChildren().setAll(chart);
            breakdownRows.getChildren().setAll(Arrays.stream(Grouping.values())
                    .map(grouping -> breakdown(grouping, StatisticsService.totals(meals, grouping), meals.size()))
                    .toList());
        }
        updatedLabel.setText("Updated " + TextFormat.time(LocalTime.now()));
        Theme.status(statusLabel, meals.isEmpty() ? "No data yet." : "Chart is up to date.",
                meals.isEmpty() ? Theme.Kind.INFO : Theme.Kind.OK);
        refreshButton.setDisable(false);
    }

    /** "Healthy level" -> "Healthy Level" */
    private static String titleCase(String text) {
        return Arrays.stream(text.split(" "))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(Collectors.joining(" "));
    }

    /** "By healthy level" with one small bar per group: its count and its share of all meals. */
    private static VBox breakdown(Grouping grouping, Map<String, Long> counts, int total) {
        Label title = new Label("By " + grouping.getLabel().toLowerCase());
        title.setStyle("-fx-font-weight: bold; -fx-text-fill: #4E0920;");
        VBox section = new VBox(8, title);
        counts.forEach((name, count) -> {
            double share = total == 0 ? 0 : (double) count / total;
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label label = new Label(name);
            label.setStyle("-fx-font-weight: bold;");
            HBox top = new HBox(label, gap, Theme.text(count + "  (" + TextFormat.percent(share) + ")", "pnp-muted"));
            ProgressBar bar = new ProgressBar(share);
            bar.getStyleClass().addAll("pnp-mini-bar", "bar-" + name.toLowerCase());
            bar.setMaxWidth(Double.MAX_VALUE);
            section.getChildren().add(new VBox(4, top, bar));
        });
        return section;
    }
}
