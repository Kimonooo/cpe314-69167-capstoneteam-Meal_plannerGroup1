package com.platenplatter.ui;

import com.platenplatter.util.Loader;
import com.platenplatter.util.MealPlanAnalyzer;
import com.platenplatter.util.StatFormat;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.chart.BarChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The "Analytics" tab.
 * What it shows: three number cards, a bar chart, and a breakdown list. All of it comes from
 * the MINED result: Loader (the database) -> MealPlanAnalyzer (counting) -> chart.
 * Nothing is hard-coded: change the data, press Refresh, and the chart changes.
 * The work runs in a background Task. Control that works: the Refresh button.
 */
public class AnalyticsScreen {
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private static class Snapshot {
        final Map<String, Integer> counts;
        final double averagePrep;

        Snapshot(Map<String, Integer> counts, double averagePrep) {
            this.counts = counts;
            this.averagePrep = averagePrep;
        }
    }

    private final Loader<List<String>> categoryLoader;
    private final Loader<List<Integer>> prepLoader;

    private final VBox root = new VBox(14);
    private final Button refreshButton = new Button("Refresh");
    private final Label updatedLabel = new Label();
    private final Label totalValue = new Label("0");
    private final Label topValue = new Label("-");
    private final Label avgValue = new Label("-");
    private final StackPane chartHolder = new StackPane();
    private final VBox breakdownRows = new VBox(10);
    private final Label emptyLabel = new Label("No meal data yet.\nImport a CSV or add a meal from the Recipes tab, then press Refresh.");
    private final Label statusLabel = new Label();

    public AnalyticsScreen(Loader<List<String>> categoryLoader, Loader<List<Integer>> prepLoader) {
        this.categoryLoader = categoryLoader;
        this.prepLoader = prepLoader;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildStatRow(), buildMainArea(), statusLabel);
        statusLabel.setId("analyticsStatus");
        Theme.apply(root);
        refreshButton.setOnAction(e -> refresh());
        refresh();
    }

    public Parent getView() {
        return root;
    }

    /** Re-reads the data, re-runs the mining step and redraws the chart (off the UI thread). */
    public void refresh() {
        refreshButton.setDisable(true);
        Theme.status(statusLabel, "Loading...", Theme.Kind.INFO);
        Task<Snapshot> task = new Task<>() {
            @Override
            protected Snapshot call() throws Exception {
                Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(categoryLoader.load());
                double avg = MealPlanAnalyzer.averagePrepMinutes(prepLoader.load());
                return new Snapshot(counts, avg);
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
        Label subtitle = new Label("What are we planning to eat? Counted straight from the database.");
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

    private HBox buildStatRow() {
        totalValue.setId("totalValue");
        topValue.setId("topValue");
        avgValue.setId("avgValue");
        HBox row = new HBox(14, statCard("TOTAL MEALS", totalValue), statCard("MOST PLANNED", topValue),
                statCard("AVERAGE PREP TIME", avgValue));
        return row;
    }

    private VBox statCard(String caption, Label valueLabel) {
        valueLabel.getStyleClass().add("pnp-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pnp-stat-label");
        VBox card = new VBox(2, cap, valueLabel);
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
        VBox breakdownCard = new VBox(12, breakdownTitle, breakdownRows);
        breakdownCard.getStyleClass().add("pnp-card");
        breakdownCard.setPrefWidth(250);
        breakdownCard.setMinWidth(220);
        breakdownCard.setMaxWidth(280);

        HBox main = new HBox(14, chartCard, breakdownCard);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    // ------------------------------------------------------------------ showing the result

    private void show(Snapshot snap) {
        int total = 0;
        for (int n : snap.counts.values()) {
            total += n;
        }
        breakdownRows.getChildren().clear();

        if (snap.counts.isEmpty()) {
            chartHolder.getChildren().setAll(emptyLabel);
            totalValue.setText("0");
            topValue.setText("-");
            avgValue.setText("-");
            Label none = new Label("Nothing to count yet.");
            none.getStyleClass().add("pnp-muted");
            breakdownRows.getChildren().add(none);
        } else {
            BarChart<String, Number> chart = AnalyticsChart.build(snap.counts);
            chartHolder.getChildren().setAll(chart);
            totalValue.setText(String.format("%,d", total));
            topValue.setText(MealPlanAnalyzer.mostFrequent(snap.counts));
            avgValue.setText(StatFormat.average(snap.averagePrep));

            List<Map.Entry<String, Integer>> sorted = new ArrayList<>(snap.counts.entrySet());
            sorted.sort((a, b) -> b.getValue().compareTo(a.getValue()));
            for (Map.Entry<String, Integer> entry : sorted) {
                breakdownRows.getChildren().add(breakdownRow(entry.getKey(), entry.getValue(), total));
            }
        }
        updatedLabel.setText("Updated " + LocalTime.now().format(CLOCK));
        Theme.status(statusLabel, snap.counts.isEmpty() ? "No data yet." : "Chart is up to date.",
                snap.counts.isEmpty() ? Theme.Kind.INFO : Theme.Kind.OK);
        refreshButton.setDisable(false);
    }

    private VBox breakdownRow(String category, int count, int total) {
        double share = total == 0 ? 0 : (double) count / total;
        Label name = new Label(category);
        name.setStyle("-fx-font-weight: bold;");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        Label numbers = new Label(String.format("%,d  (%.0f%%)", count, share * 100));
        numbers.getStyleClass().add("pnp-muted");
        HBox top = new HBox(name, spacer, numbers);

        ProgressBar bar = new ProgressBar(share);
        bar.getStyleClass().add("pnp-mini-bar");
        bar.setMaxWidth(Double.MAX_VALUE);
        return new VBox(4, top, bar);
    }
}
