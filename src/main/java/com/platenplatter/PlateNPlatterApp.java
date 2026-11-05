package com.platenplatter;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanHistoryDao;
import com.platenplatter.ui.AppShell;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;

/**
 * The Plate N Platter application. Run it with:   mvn javafx:run
 * Opens the database file platenplatter.db, creates the tables if needed, and shows the window:
 * live date and clock, streak, and the tabs Today, Meal Plan, Recipes, Shopping List, Import, Analytics.
 */
public class PlateNPlatterApp extends Application {
    private Connection connection;
    private AppShell.Shell shell;

    @Override
    public void start(Stage stage) {
        try {
            connection = DatabaseConnector.open();
            MealPlanHistoryDao dao = new MealPlanHistoryDao(connection);
            dao.initSchema();
            shell = AppShell.build(dao, new MealDbClient());

            Scene scene = new Scene(shell.root(), 1120, 840);
            stage.setTitle("Plate N Platter");
            stage.setMinWidth(980);
            stage.setMinHeight(700);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            showError(stage, e);
        }
    }

    @Override
    public void stop() throws Exception {
        if (shell != null) {
            shell.stop();
        }
        if (connection != null) {
            connection.close();
        }
    }

    private static void showError(Stage stage, Exception e) {
        Label title = new Label("Plate N Platter could not start");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #B3261E;");
        Label detail = new Label("Reason: " + e.getMessage()
                + "\n\nCheck that the database file platenplatter.db is in the project folder and not locked by another program.");
        detail.setWrapText(true);
        VBox box = new VBox(12, title, detail);
        box.setPadding(new Insets(28));
        stage.setTitle("Plate N Platter - problem");
        stage.setScene(new Scene(box, 640, 260));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
