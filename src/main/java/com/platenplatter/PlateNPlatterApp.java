package com.platenplatter;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.controller.AnalyticsController;
import com.platenplatter.controller.AuthController;
import com.platenplatter.controller.ImportController;
import com.platenplatter.controller.MealPlanController;
import com.platenplatter.controller.RecipeController;
import com.platenplatter.controller.ShoppingListController;
import com.platenplatter.controller.TodayController;
import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanDao;
import com.platenplatter.db.PlanStore;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.service.RecommendationService;
import com.platenplatter.ui.AppShell;
import com.platenplatter.ui.ForgotPasswordScreen;
import com.platenplatter.ui.LoginScreen;
import com.platenplatter.ui.MealPhotos;
import com.platenplatter.ui.RegisterScreen;
import com.platenplatter.ui.Theme;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

/**
 * Starts Plate N Platter. This is the COMPOSITION ROOT: the only place that opens the database
 * and makes the DAOs, the services and the controllers, and the only owner of the background
 * thread pool. It opens plate_n_platter.db (creating the 5 tables the first time), shows Log in,
 * and after log-in builds the pages of that one user.
 * Run it with: mvn javafx:run  (or in Eclipse: Run As > Maven build... > javafx:run)
 */
public class PlateNPlatterApp extends Application {
    /** The size the window would like; it is made smaller on a small screen (1366 x 768). */
    private static final double WIDTH = 1180;
    private static final double HEIGHT = 820;
    /** How much of the screen the window may take at most. */
    private static final double SCREEN_SHARE = 0.92;
    private static final Logger LOG = Logger.getLogger("PlateNPlatter");

    private Stage stage;
    private Connection connection;
    private AuthController auth;
    private final MealDbClient client = new MealDbClient();
    private final RecommendationService recommendations = new RecommendationService(client);
    private final ExecutorService workers = AppShell.newWorkers();
    private AppShell shell;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        AppShell.useWorkers(workers);                    // one named pool for every background job
        MealPhotos.useClient(client);                    // the photos use the same TheMealDB client
        stage.setTitle("Plate N Platter");
        try {
            connection = DatabaseConnector.open();
            // safe every time: CREATE TABLE IF NOT EXISTS
            DatabaseConnector.createTables(connection);
            auth = new AuthController(new AuthService(new UserDao(connection)));
            showLogin(null, null);
        } catch (SQLException e) {
            LOG.log(Level.SEVERE, "The database could not be opened", e);
            setPage(Theme.errorPage("Plate N Platter could not start",
                    "The meal database (plate_n_platter.db) could not be opened. Close any program "
                    + "that has it open, such as a database viewer, then start the app again."));
        }
        placeOnScreen();
        stage.show();
    }

    @Override
    public void stop() throws SQLException {
        closeShell();
        workers.shutdownNow();                           // the pool ends with the window
        if (connection != null) {
            connection.close();
        }
    }

    /**
     * Fits the window on the screen the user has (taskbar excluded) and centers it there, so the
     * title bar is never above the screen and the bottom is never under the taskbar.
     */
    private void placeOnScreen() {
        Rectangle2D screen = Screen.getPrimary().getVisualBounds();
        double width = Math.min(WIDTH, screen.getWidth() * SCREEN_SHARE);
        double height = Math.min(HEIGHT, screen.getHeight() * SCREEN_SHARE);
        stage.setMinWidth(Math.min(980, width));
        stage.setMinHeight(Math.min(600, height));
        stage.setWidth(width);
        stage.setHeight(height);
        stage.setX(screen.getMinX() + (screen.getWidth() - width) / 2);
        stage.setY(screen.getMinY() + (screen.getHeight() - height) / 2);
    }

    private void showLogin(String username, String notice) {
        setPage(new LoginScreen(auth, this::openAppFor, this::showRegister,
                this::showForgotPassword,
                username, notice).getView());
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
        PlanStore store = new MealPlanDao(connection, user.getUserId());
        AppShell.Controllers controllers = new AppShell.Controllers(new TodayController(store),
                new RecipeController(client, recommendations, store), new MealPlanController(store),
                new ShoppingListController(store, client), new ImportController(store),
                new AnalyticsController(store), auth);
        shell = new AppShell(controllers, user, this::logout, this::accountDeleted);
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

    public static void main(String[] args) {
        launch(args);
    }
}
