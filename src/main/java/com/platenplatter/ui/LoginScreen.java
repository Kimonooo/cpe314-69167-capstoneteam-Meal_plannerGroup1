package com.platenplatter.ui;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthException;
import com.platenplatter.service.AuthService;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * The first page of the app: log in with a username and password.
 * Checking the password (PBKDF2 hashing) runs in a background Task, so the window never freezes.
 * Controls that work: the two fields (Enter logs in), Log in, Create an account.
 */
public class LoginScreen {
    private final AuthService auth;
    private final Consumer<User> onLoggedIn;

    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Log in");
    private final Button createLink = AuthLayout.link("Create an account");
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    /**
     * @param username a username to fill in (for example right after signing up), or null
     * @param notice   a green message to show (for example "Account created!"), or null
     */
    public LoginScreen(AuthService auth, Consumer<User> onLoggedIn, Runnable onCreateAccount,
                       String username, String notice) {
        this.auth = auth;
        this.onLoggedIn = onLoggedIn;

        usernameField.setId("loginUsername");
        usernameField.setPromptText("Your username");
        usernameField.getStyleClass().add("pnp-field");
        passwordField.setId("loginPassword");
        passwordField.setPromptText("Your password");
        passwordField.getStyleClass().add("pnp-field");

        loginButton.setId("loginButton");
        loginButton.getStyleClass().add("pnp-button");
        loginButton.setDefaultButton(true);
        loginButton.disableProperty().bind(working);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(loginButton, Priority.ALWAYS);

        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(22, 22);
        busy.setMaxSize(22, 22);
        busy.visibleProperty().bind(working);
        busy.managedProperty().bind(working);
        HBox buttonRow = new HBox(10, loginButton, busy);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        messageLabel.setId("loginMessage");
        messageLabel.setWrapText(true);

        createLink.setId("createAccountLink");
        createLink.disableProperty().bind(working);
        createLink.setOnAction(e -> onCreateAccount.run());
        Label noAccount = new Label("New here?");
        noAccount.getStyleClass().add("pnp-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox createRow = new HBox(6, noAccount, createLink, spacer);
        createRow.setAlignment(Pos.CENTER_LEFT);

        root = AuthLayout.page("Log in", "Welcome back! Log in to see your meal plan.",
                AuthLayout.labeled("Username", usernameField),
                AuthLayout.labeled("Password", passwordField),
                buttonRow, messageLabel, createRow);

        loginButton.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> {
            e.consume(); // Enter in the username box only moves to the password box (no log-in attempt yet)
            passwordField.requestFocus();
        });

        if (username != null) {
            usernameField.setText(username);
        }
        if (notice != null) {
            Theme.status(messageLabel, notice, Theme.Kind.OK);
        }
        Platform.runLater(() -> (username == null ? usernameField : passwordField).requestFocus());
    }

    public Parent getView() {
        return root;
    }

    private void handleLogin() {
        String username = usernameField.getText();
        String password = passwordField.getText();
        working.set(true);
        Theme.status(messageLabel, "Checking...", Theme.Kind.INFO);

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.login(username, password);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            onLoggedIn.accept(task.getValue());
        });
        task.setOnFailed(e -> {
            working.set(false);
            passwordField.clear();
            passwordField.requestFocus();
            Throwable ex = task.getException();
            String text = ex instanceof AuthException ? ex.getMessage()
                    : "Database problem: " + (ex == null ? "unknown error" : ex.getMessage());
            Theme.status(messageLabel, text, Theme.Kind.ERROR);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
