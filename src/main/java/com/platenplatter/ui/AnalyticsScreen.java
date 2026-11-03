package com.platenplatter.ui;

import com.platenplatter.util.Loader;
import com.platenplatter.util.MealPlanAnalyzer;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;

/**
 * "Analytics" tab. Responsibility: show the MINED result as a labeled bar chart.
 * Data comes from Loaders (the database, later) -> MealPlanAnalyzer -> AnalyticsChart.
 * Nothing is hard-coded: press Refresh after the data changes and the chart changes.
 * Control (wired): Refresh button.
 */
public class AnalyticsScreen {
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

    private final VBox root = new VBox(12);
    private final StackPane chartHolder = new StackPane();
    private final Button refreshButton = new Button("Refresh");
    private final Label summary = new Label();

    public AnalyticsScreen(Loader<List<String>> categoryLoader, Loader<List<Integer>> prepLoader) {
        this.categoryLoader = categoryLoader;
        this.prepLoader = prepLoader;

        Label title = new Label("Meal Plan Analytics");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5C0008;");
        refreshButton.setStyle("-fx-background-color: #5C0008; -fx-text-fill: white;");
        VBox.setVgrow(chartHolder, Priority.ALWAYS);

        root.getChildren().addAll(title, refreshButton, summary, chartHolder);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFBD0;");

        refreshButton.setOnAction(e -> refresh());
        refresh();
    }

    public Parent getView() {
        return root;
    }

    /** Re-reads the data, re-runs the mining step, redraws the chart (off the UI thread). */
    public void refresh() {
        refreshButton.setDisable(true);
        summary.setText("Loading...");
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
            summary.setText("Could not load data: " + (ex == null ? "unknown error" : ex.getMessage()));
            refreshButton.setDisable(false);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void show(Snapshot snap) {
        chartHolder.getChildren().setAll(AnalyticsChart.build(snap.counts));
        if (snap.counts.isEmpty()) {
            summary.setText("No meal data yet. Import a CSV or add meals from the Recipes tab, then press Refresh.");
        } else {
            int total = 0;
            for (int n : snap.counts.values()) {
                total += n;
            }
            summary.setText(String.format(
                    "Most planned category: %s   |   Total meals: %d   |   Average prep time: %.1f min",
                    MealPlanAnalyzer.mostFrequent(snap.counts), total, snap.averagePrep));
        }
        refreshButton.setDisable(false);
    }
}
