package com.platenplatter.ui;

import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.Map;

/** Builds the labeled bar chart from the MINED result (category -> count). */
public final class AnalyticsChart {
    private AnalyticsChart() { }

    public static BarChart<String, Number> build(Map<String, Integer> counts) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Recipe category");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Number of meals planned");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Meals Planned per Category");
        chart.setAnimated(false);
        chart.setLegendVisible(true);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Meals planned");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chart.getData().add(series);
        return chart;
    }
}
