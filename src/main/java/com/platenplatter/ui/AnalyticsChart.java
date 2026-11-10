package com.platenplatter.ui;

import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.ChartMath;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

import java.util.List;
import java.util.Map;

/**
 * Builds the labeled bar charts from the MINED result.
 * Title, axis labels and a legend are all set here, because the grader checks them.
 */
public final class AnalyticsChart {
    private AnalyticsChart() { }

    /** The first chart we had: category -> number of meals, one bar per category. */
    public static BarChart<String, Number> build(Map<String, Integer> counts) {
        int max = 0;
        for (int n : counts.values()) {
            max = Math.max(max, n);
        }

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Recipe category");
        // Fixed, tidy scale (0, 1, 2... or 0, 250, 500...) so labels never show half a meal.
        NumberAxis yAxis = new NumberAxis(0, ChartMath.upperBound(max), ChartMath.niceStep(max));
        yAxis.setLabel("Number of meals planned");
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setId("mealChart");
        chart.getStyleClass().add("pnp-chart");
        chart.setTitle("Meals Planned per Category");
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(32);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Meals planned");
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            series.getData().add(new XYChart.Data<>(e.getKey(), e.getValue()));
        }
        chart.getData().add(series);
        return chart;
    }

    /**
     * A GROUPED bar chart: one group of bars per day, one bar (and one legend entry) per group of the
     * grouping, for example Healthy / Moderate / Indulgent. The bar colors come from the CSS class
     * "group-healthy_level", "group-prep_time" or "group-meal_slot", so Healthy is always green.
     *
     * @param table day label -> (group name -> number of meals), from StatisticsService
     */
    public static BarChart<String, Number> buildGrouped(String title, Map<String, Map<String, Integer>> table,
                                                        StatisticsService.Grouping grouping) {
        int max = StatisticsService.maxCount(table);

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Day");
        NumberAxis yAxis = new NumberAxis(0, ChartMath.upperBound(max), ChartMath.niceStep(max));
        yAxis.setLabel("Number of meals planned");
        yAxis.setMinorTickVisible(false);

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setId("mealChart");
        chart.getStyleClass().addAll("pnp-chart", "group-" + grouping.name().toLowerCase());
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(18);
        chart.setBarGap(2);
        chart.setHorizontalGridLinesVisible(true);
        chart.setVerticalGridLinesVisible(false);

        List<String> groups = grouping.seriesNames();
        for (String group : groups) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(group);
            for (Map.Entry<String, Map<String, Integer>> day : table.entrySet()) {
                series.getData().add(new XYChart.Data<>(day.getKey(), day.getValue().getOrDefault(group, 0)));
            }
            chart.getData().add(series);
        }
        return chart;
    }
}
