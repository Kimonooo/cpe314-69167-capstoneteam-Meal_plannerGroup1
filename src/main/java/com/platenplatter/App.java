package com.platenplatter;

import com.platenplatter.ui.RegisterScreen;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Starts the JavaFX window. */
public class App extends Application {

    @Override
    public void start(Stage stage) {
        RegisterScreen registerScreen = new RegisterScreen();
        Scene scene = new Scene(registerScreen.getView(), 560, 560);
        stage.setTitle("Plate N Platter");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
