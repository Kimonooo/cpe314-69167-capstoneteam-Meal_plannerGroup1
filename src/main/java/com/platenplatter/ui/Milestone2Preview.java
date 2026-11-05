package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.InMemoryPlanStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The whole application on a THROW-AWAY memory store: nothing is saved to the database file, so you can
 * click around freely. (Recipe search still uses the internet.) Run:
 *   mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.Milestone2Preview
 */
public class Milestone2Preview extends Application {
    private AppShell.Shell shell;

    @Override
    public void start(Stage stage) {
        shell = AppShell.build(new InMemoryPlanStore(), new MealDbClient());
        stage.setTitle("Plate N Platter - preview (nothing is saved)");
        stage.setScene(new Scene(shell.root(), 1120, 840));
        stage.show();
    }

    @Override
    public void stop() {
        if (shell != null) {
            shell.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
