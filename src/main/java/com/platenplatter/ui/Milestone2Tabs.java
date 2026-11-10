package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanHistoryDao;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

/**
 * For the OLD App.java only (the one with the Sign up and Calendar tabs). Adds the six new tabs with ONE line:
 *
 *     tabPane.getTabs().addAll(Milestone2Tabs.createWithDefaultDatabase());
 *
 * To run the finished application instead (with the live header), use PlateNPlatterApp.
 */
public final class Milestone2Tabs {
    private Milestone2Tabs() { }

    /** Opens platenplatter.db and never throws: a problem becomes a red "Setup problem" tab. */
    public static List<Tab> createWithDefaultDatabase() {
        try {
            MealPlanHistoryDao dao = new MealPlanHistoryDao(DatabaseConnector.open());
            dao.initSchema();
            AppShell.Shell shell = AppShell.build(dao, new MealDbClient());
            List<Tab> tabs = new ArrayList<>(shell.tabs().getTabs());
            shell.tabs().getTabs().clear(); // a Tab can only live in one TabPane
            return tabs;
        } catch (Exception e) {
            return List.of(problemTab(e));
        }
    }

    private static Tab problemTab(Exception e) {
        Label title = new Label("The new tabs could not start");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #B3261E;");
        Label detail = new Label("Reason: " + e.getMessage()
                + "\n\nCheck that the database file platenplatter.db can be opened, then restart the app.");
        detail.setWrapText(true);
        VBox box = new VBox(10, title, detail);
        box.setPadding(new Insets(24));
        Tab tab = new Tab("Setup problem", box);
        tab.setClosable(false);
        return tab;
    }
}
