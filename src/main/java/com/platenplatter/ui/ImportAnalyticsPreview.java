package com.platenplatter.ui;

import com.platenplatter.util.InMemoryPlanStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/**
 * The Import and Analytics tabs together, on a throw-away memory store (nothing is saved).
 * Run on Kimberly's laptop:
 *   mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.ImportAnalyticsPreview
 * Try: import sample_data/meal_history_30000.csv, then open the Analytics tab.
 */
public class ImportAnalyticsPreview extends Application {
    @Override
    public void start(Stage stage) {
        InMemoryPlanStore store = new InMemoryPlanStore();
        ImportScreen importScreen = new ImportScreen(store::addImported);
        AnalyticsScreen analytics = new AnalyticsScreen(store::categories, store::prepMinutes);
        importScreen.setOnImportFinished(analytics::refresh);

        TabPane tabs = new TabPane();
        tabs.getStyleClass().add("pnp-tabs");
        Tab importTab = new Tab("Import", importScreen.getView());
        Tab analyticsTab = new Tab("Analytics", analytics.getView());
        analyticsTab.setOnSelectionChanged(e -> {
            if (analyticsTab.isSelected()) {
                analytics.refresh();
            }
        });
        tabs.getTabs().addAll(importTab, analyticsTab);
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Theme.apply(tabs);
        stage.setTitle("Import + Analytics preview");
        stage.setScene(new Scene(tabs, 960, 780));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
