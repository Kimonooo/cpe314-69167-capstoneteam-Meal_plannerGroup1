package com.platenplatter.ui;

import com.platenplatter.controller.AnalyticsController;
import com.platenplatter.controller.AnalyticsController.Snapshot;
import com.platenplatter.controller.UserMessages;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;
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
 * The Analytics page: the user's real meals are read in the background, mined by
 * AnalyticsController (with StatisticsService), and drawn as a grouped bar chart with four
 * summary cards and a breakdown panel. Week / All time, the week arrows, the grouping box and
 * Refresh sit on one row above the cards. Only the newest answer is drawn.
 */
public class AnalyticsScreen {
    private final AnalyticsController controller;
    private final Supplier<LocalDate> todayProvider;
    private LocalDate monday;
    private int request; // the newest read; older ones are ignored
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

    public AnalyticsScreen(AnalyticsController controller) {
        this(controller, LocalDate::now);
    }

    public AnalyticsScreen(AnalyticsController controller, Supplier<LocalDate> todayProvider) {
        this.controller = controller;
        this.todayProvider = todayProvider;
        this.monday = AnalyticsController.mondayOf(todayProvider.get());
        refreshButton.setId("refreshButton");
        refreshButton.getStyleClass().add("pnp-button-secondary");
        refreshButton.setOnAction(e -> refresh());
        updatedLabel.getStyleClass().add("pnp-hint");
        totalValue.setId("totalValue");
        healthyValue.setId("healthyValue");
        avgValue.setId("avgValue");
        caloriesValue.setId("caloriesValue");
        HBox stats = new HBox(14, Theme.statCard("MEALS", totalValue, totalUnit, "dishes"),
                Theme.statCard("HEALTHY SHARE", healthyValue, new Label("of meals are Healthy"),
                        "food"),
                Theme.statCard("AVERAGE PREP TIME", avgValue, new Label("per meal"), "hat"),
                Theme.statCard("AVERAGE CALORIES", caloriesValue, caloriesUnit, "streak"));
        statusLabel.setId("analyticsStatus");
        VBox content = new VBox(14, buildControls(), stats, buildMainArea(), statusLabel);
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
        Grouping grouping = groupingBox.getValue() == null ? Grouping.HEALTHY_LEVEL
                : groupingBox.getValue();
        weekLabel.setText("Week of " + AnalyticsController.weekLabel(shownMonday));
        int mine = ++request;
        Theme.status(statusLabel, "Loading...", Theme.Kind.INFO);
        Task<Snapshot> task = new Task<>() {
            @Override
            protected Snapshot call() throws Exception {
                return controller.load(weekScope, shownMonday, grouping);
            }
        };
        task.setOnSucceeded(e -> {
            if (mine == request) { // the user changed week or grouping meanwhile
                show(task.getValue());
            }
        });
        task.setOnFailed(e -> {
            if (mine == request) {
                Theme.status(statusLabel, UserMessages.of(task.getException()), Theme.Kind.ERROR);
            }
        });
        AppShell.runInBackground(task);
    }

    /** One row: Week / All time, the week arrows, the grouping box, then Refresh and its time. */
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
        Button thisWeek = weekButton("This week", "thisWeek",
                () -> monday = AnalyticsController.mondayOf(todayProvider.get()));
        weekLabel.setId("weekLabel");
        weekLabel.getStyleClass().add("pnp-week-title");
        weekLabel.setMinWidth(170);
        weekLabel.setAlignment(Pos.CENTER);
        weekNav.getChildren().addAll(prev, weekLabel, next, thisWeek);
        weekNav.setAlignment(Pos.CENTER_LEFT);
        groupingBox.setId("groupingBox");
        groupingBox.getItems().addAll(Grouping.values());
        groupingBox.setValue(Grouping.HEALTHY_LEVEL);
        groupingBox.valueProperty().addListener((obs, old, now) -> refresh());
        Label groupLabel = Theme.text("Group bars by", "pnp-field-label");
        groupLabel.setLabelFor(groupingBox);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        VBox refreshBox = new VBox(2, refreshButton, updatedLabel);
        refreshBox.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(10, weekToggle, allToggle, weekNav, gap, groupLabel, groupingBox,
                refreshBox);
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
        healthyValue.setText(TextFormat.percent(snap.healthyShare()));
        avgValue.setText(TextFormat.averageMinutes(snap.averagePrepMinutes()));
        long withCalories = snap.withCalories();
        caloriesValue.setText(withCalories == 0 ? "-"
                : TextFormat.calories((int) Math.round(snap.averageCalories())));
        caloriesUnit.setText(withCalories == 0 ? "no calories given yet"
                : "per meal (" + withCalories + " with calories)");
        if (snap.isEmpty()) {
            VBox empty = Theme.emptyState(snap.weekScope()
                    ? "No meals planned in the week of "
                            + AnalyticsController.weekLabel(snap.monday())
                            + ".\nAdd a meal on Recipes, or try another week."
                    : "No meal data yet.\nImport a CSV or add a meal on Recipes.", "dishes");
            chartHolder.getChildren().setAll(empty);
            breakdownRows.getChildren().setAll(Theme.text("Nothing to count yet.", "pnp-muted"));
        } else {
            BarChart<String, Number> chart = AnalyticsChart.buildGrouped(snap.chartTitle(),
                    snap.xAxisLabel(), snap.table(), snap.grouping());
            chartHolder.getChildren().setAll(chart);
            breakdownRows.getChildren().setAll(snap.breakdown().entrySet().stream()
                    .map(entry -> breakdown(entry.getKey(), entry.getValue(), meals.size()))
                    .toList());
        }
        updatedLabel.setText("Updated " + TextFormat.time(LocalTime.now()));
        Theme.status(statusLabel, meals.isEmpty() ? "No data yet." : "Chart is up to date.",
                meals.isEmpty() ? Theme.Kind.INFO : Theme.Kind.OK);
    }

    /** "By healthy level" with one small bar per group: its count and its share of all meals. */
    private static VBox breakdown(Grouping grouping, Map<String, Long> counts, int total) {
        Label title = new Label("By " + grouping.getLabel().toLowerCase(Locale.ROOT));
        title.getStyleClass().add("pnp-breakdown-title");
        VBox section = new VBox(8, title);
        counts.forEach((name, count) -> {
            double share = total == 0 ? 0 : (double) count / total;
            Region gap = new Region();
            HBox.setHgrow(gap, Priority.ALWAYS);
            Label label = Theme.text(name, "pnp-breakdown-name");
            HBox top = new HBox(label, gap, Theme.text(count + "  (" + TextFormat.percent(share)
                    + ")",
                    "pnp-muted"));
            ProgressBar bar = new ProgressBar(share);
            bar.getStyleClass().addAll("pnp-mini-bar", "bar-" + name.toLowerCase(Locale.ROOT));
            bar.setMaxWidth(Double.MAX_VALUE);
            section.getChildren().add(new VBox(4, top, bar));
        });
        return section;
    }
}
