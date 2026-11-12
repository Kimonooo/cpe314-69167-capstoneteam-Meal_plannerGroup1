package com.platenplatter;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanDao;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.ui.AppShell;
import com.platenplatter.ui.ForgotPasswordScreen;
import com.platenplatter.ui.LoginScreen;
import com.platenplatter.ui.RegisterScreen;
import java.sql.Connection;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * Starts Plate N Platter: opens plate_n_platter.db (creating the 5 tables the first time),
 * shows Log in, and after log-in opens the pages for that one user.
 * Run it with: mvn javafx:run  (or in Eclipse: Run As > Maven build... > javafx:run)
 */
public class PlateNPlatterApp extends Application {
    private static final double WIDTH = 1180;
    private static final double HEIGHT = 860;

    private Stage stage;
    private Connection connection;
    private AuthService auth;
    private final MealDbClient client = new MealDbClient();
    private AppShell shell;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            connection = DatabaseConnector.open();
            DatabaseConnector.createTables(connection);     // safe every time: CREATE TABLE IF NOT EXISTS
            auth = new AuthService(new UserDao(connection));
            stage.setTitle("Plate N Platter");
            stage.setMinWidth(1040);
            stage.setMinHeight(720);
            showLogin(null, null);
            stage.show();
        } catch (Exception e) {
            showError(e);
        }
    }

    @Override
    public void stop() throws Exception {
        closeShell();
        if (connection != null) {
            connection.close();
        }
    }

    private void showLogin(String username, String notice) {
        setPage(new LoginScreen(auth, this::openAppFor, this::showRegister, this::showForgotPassword, username, notice).getView());
    }

    private void showRegister() {
        setPage(new RegisterScreen(auth,
                username -> showLogin(username, "Account created! Log in with your new password."),
                () -> showLogin(null, null)).getView());
    }

    private void showForgotPassword() {
        setPage(new ForgotPasswordScreen(auth,
                username -> showLogin(username, "Password changed. You can log in now."),
                () -> showLogin(null, null)).getView());
    }

    /** One MealPlanDao per logged-in user, so every query only sees that user's meals. */
    private void openAppFor(User user) {
        closeShell();
        shell = new AppShell(new MealPlanDao(connection, user.getUserId()), client, user, auth, this::logout,
                this::accountDeleted);
        setPage(shell.getView());
    }

    private void logout() {
        String username = shell == null ? null : shell.getUser().getUsername();
        closeShell();
        showLogin(username, "You are logged out.");
    }

    /** After "Delete my account": stop the pages of the deleted user and go back to Log in. */
    private void accountDeleted() {
        closeShell();
        showLogin(null, "Your account and all its data were deleted.");
    }

    private void closeShell() {
        if (shell != null) {
            shell.stop();
            shell = null;
        }
    }

    private void setPage(Parent page) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(page, WIDTH, HEIGHT));
        } else {
            stage.getScene().setRoot(page);
        }
    }

    private void showError(Exception e) {
        Label title = new Label("Plate N Platter could not start");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #B3261E;");
        Label detail = new Label("Reason: " + e.getMessage() + "\n\nCheck that plate_n_platter.db is not open in another program.");
        detail.setWrapText(true);
        VBox box = new VBox(12, title, detail);
        box.setPadding(new Insets(28));
        stage.setScene(new Scene(box, 640, 260));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
