package com.platenplatter.ui;

import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.ChartMath;
import java.util.Map;
import javafx.geometry.Side;
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

    /**
     * xAxisLabel is "Day" for one week (the seven dates) and "Weekday" for all time (Mon to Sun).
     * The legend sits at the top, so it is never pushed below the window.
     */
    public static BarChart<String, Number> buildGrouped(String title, String xAxisLabel,
            Map<String, Map<String, Long>> table, StatisticsService.Grouping grouping) {
        long max = StatisticsService.maxCount(table);
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel(xAxisLabel);
        NumberAxis yAxis = new NumberAxis(0, ChartMath.upperBound(max), ChartMath.niceStep(max));
        yAxis.setLabel("Number of meals planned");
        yAxis.setMinorTickVisible(false);
        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setId("mealChart");
        // same colors as the chips
        chart.getStyleClass().addAll("pnp-chart", "group-" + grouping.name().toLowerCase());
        chart.setTitle(title);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.TOP);
        chart.setMinHeight(300);
        chart.setPrefHeight(340);
        chart.setCategoryGap(18);
        chart.setBarGap(2);
        chart.setVerticalGridLinesVisible(false);
        grouping.seriesNames().forEach(group -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(group);
            table.forEach((day, counts) -> series.getData()
                    .add(new XYChart.Data<>(day, counts.getOrDefault(group, 0L))));
            chart.getData().add(series);
        });
        return chart;
    }
}
