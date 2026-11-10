package com.platenplatter.ui;

import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.ChartMath;
import java.util.Map;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

/**
 * The DATA VISUALIZATION: a labeled, grouped bar chart. One series per group (for example Healthy,
 * Moderate, Indulgent), one bar per day, with a title, axis labels and a legend.
 */
public final class AnalyticsChart {
    private AnalyticsChart() { }

    public static BarChart<String, Number> buildGrouped(String title, Map<String, Map<String, Long>> table,
                                                        StatisticsService.Grouping grouping) {
        long max = StatisticsService.maxCount(table);
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Day");
        NumberAxis yAxis = new NumberAxis(0, ChartMath.upperBound(max), ChartMath.niceStep(max));
        yAxis.setLabel("Number of meals planned");
        yAxis.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setId("mealChart");
        chart.getStyleClass().addAll("pnp-chart", "group-" + grouping.name().toLowerCase()); // same colors as the chips
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setCategoryGap(18);
        chart.setBarGap(2);
        chart.setVerticalGridLinesVisible(false);
        grouping.seriesNames().forEach(group -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(group);
            table.forEach((day, counts) -> series.getData().add(new XYChart.Data<>(day, counts.getOrDefault(group, 0L))));
            chart.getData().add(series);
        });
        return chart;
    }
}
