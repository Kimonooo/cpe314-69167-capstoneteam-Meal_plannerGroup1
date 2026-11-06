package com.platenplatter;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanHistoryDao;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.ui.AppShell;
import com.platenplatter.ui.LoginScreen;
import com.platenplatter.ui.RegisterScreen;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.sql.Connection;

/**
 * The Plate N Platter application. Run it with:   mvn javafx:run
 *
 * What happens:
 *   1. Opens the database file platenplatter.db and creates (or upgrades) the tables.
 *   2. Shows the LOG IN page. "Create an account" opens the SIGN UP page; after signing up the
 *      user is sent back to log in with the username filled in.
 *   3. After a good log in, opens the app FOR THAT USER: the live header (greeting with the display
 *      name, clock, streak badge) and the tabs Today, Meal Plan, Recipes, Shopping List, Import,
 *      Analytics and Account. Every tab only sees that user's meals.
 *   4. Log out (header button or Account tab) goes back to the log-in page.
 */
public class PlateNPlatterApp extends Application {
    private static final double WIDTH = 1120;
    private static final double HEIGHT = 840;

    private Stage stage;
    private Connection connection;
    private AuthService auth;
    private final MealDbClient client = new MealDbClient();
    private AppShell.Shell shell;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        try {
            connection = DatabaseConnector.open();
            new MealPlanHistoryDao(connection).initSchema(); // users + meal tables, safe to run every time
            auth = new AuthService(new UserDao());

            stage.setTitle("Plate N Platter");
            stage.setMinWidth(980);
            stage.setMinHeight(700);
            showLogin(null, null);
            stage.show();
        } catch (Exception e) {
            showError(stage, e);
        }
    }

    @Override
    public void stop() throws Exception {
        closeShell();
        if (connection != null) {
            connection.close();
        }
    }

    // ------------------------------------------------------------------ the pages

    private void showLogin(String username, String notice) {
        LoginScreen login = new LoginScreen(auth, this::openAppFor, this::showRegister, username, notice);
        setPage(login.getView());
        stage.setTitle("Plate N Platter - Log in");
    }

    private void showRegister() {
        RegisterScreen register = new RegisterScreen(auth,
                username -> showLogin(username, "Account created! Log in with your new password."),
                () -> showLogin(null, null));
        setPage(register.getView());
        stage.setTitle("Plate N Platter - Sign up");
    }

    /**
     * Opens the app for one user. First (in the background) the meals saved before accounts existed
     * are given to this user, so an old database file keeps its meals.
     */
    private void openAppFor(User user) {
        MealPlanHistoryDao dao = new MealPlanHistoryDao(connection, user.getId());
        Task<Integer> claim = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return dao.claimUnownedMeals();
            }
        };
        claim.setOnSucceeded(e -> showShell(user, dao));
        claim.setOnFailed(e -> showShell(user, dao)); // the old rows just stay unclaimed; nothing is lost
        Thread thread = new Thread(claim);
        thread.setDaemon(true);
        thread.start();
    }

    private void showShell(User user, MealPlanHistoryDao dao) {
        closeShell();
        shell = AppShell.build(dao, client, user, auth, this::logout);
        setPage(shell.root());
        stage.setTitle("Plate N Platter"); // the user's name is in the header greeting (it can change)
    }

    private void logout() {
        String username = shell == null ? null : shell.user().getUsername();
        closeShell();
        showLogin(username, "You are logged out.");
    }

    private void closeShell() {
        if (shell != null) {
            shell.stop();
            shell = null;
        }
    }

    /** Puts a page in the window (the first page creates the Scene; later pages only swap its root). */
    private void setPage(Parent page) {
        if (stage.getScene() == null) {
            stage.setScene(new Scene(page, WIDTH, HEIGHT));
        } else {
            stage.getScene().setRoot(page);
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
