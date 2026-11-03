package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Lets Nesley test the Recipes tab ALONE (no database needed).
 * "Add to plan" just prints to the console. Run on the LAPTOP:
 *   mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.RecipesScreenPreview
 */
public class RecipesScreenPreview extends Application {
    @Override
    public void start(Stage stage) {
        RecipesScreen screen = new RecipesScreen(new MealDbClient(),
                row -> System.out.println("WOULD SAVE: " + row.getDate() + " | " + row.getRecipeName()
                        + " | " + row.getCategory() + " | " + row.getPrepMinutes() + " min"));
        stage.setTitle("Recipes tab preview");
        stage.setScene(new Scene(screen.getView(), 760, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
