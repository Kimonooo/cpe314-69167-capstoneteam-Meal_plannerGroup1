package com.platenplatter;

import com.platenplatter.db.Database;
import com.platenplatter.ui.CalendarScreen;
import com.platenplatter.ui.RegisterScreen;
import java.sql.SQLException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

/** Starts the JavaFX window: sets up the database, then shows the sign-up and calendar tabs. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        try {
            Database.initialize(); // creates the tables the first time the app runs
        } catch (SQLException e) {
            System.err.println("Could not set up the database: " + e.getMessage());
        }

        Tab registerTab = new Tab("Sign up", new RegisterScreen().getView());
        Tab calendarTab = new Tab("Calendar", new CalendarScreen().getView());
        registerTab.setClosable(false);
        calendarTab.setClosable(false);

        TabPane tabs = new TabPane();
        tabs.getTabs().addAll(registerTab, calendarTab);

        stage.setTitle("Plate N Platter");
        stage.setScene(new Scene(tabs, 640, 640));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
