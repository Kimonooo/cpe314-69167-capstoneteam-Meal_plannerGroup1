package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.InMemoryPlanStore;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * The Recipes tab ALONE, on a throw-away memory store (nothing is saved). The recipe search still uses
 * the internet. Run on Kimberly's laptop:
 *   mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.RecipesScreenPreview
 */
public class RecipesScreenPreview extends Application {
    @Override
    public void start(Stage stage) {
        RecipesScreen screen = new RecipesScreen(new MealDbClient(), new InMemoryPlanStore());
        stage.setTitle("Recipes tab preview");
        stage.setScene(new Scene(screen.getView(), 960, 760));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
