package com.platenplatter.ui;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** The first page: log in, or go to Sign up or Forgot password. Checking runs in the background. */
public class LoginScreen {
    private final AuthService auth;
    private final Consumer<User> onLoggedIn;
    private final TextField usernameField = new TextField();
    private final PasswordField passwordField = new PasswordField();
    private final Button loginButton = new Button("Log in");
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    public LoginScreen(AuthService auth, Consumer<User> onLoggedIn, Runnable onCreateAccount,
                       Runnable onForgotPassword, String username, String notice) {
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
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.disableProperty().bind(working);
        messageLabel.setId("loginMessage");
        messageLabel.setWrapText(true);

        Button forgotLink = AuthLayout.link("Forgot password?");
        forgotLink.setId("forgotPasswordLink");
        forgotLink.disableProperty().bind(working);
        forgotLink.setOnAction(e -> onForgotPassword.run());
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox forgotRow = new HBox(gap, forgotLink);           // the link sits under the box, on the right

        Button createLink = AuthLayout.link("Create an account");
        createLink.setId("createAccountLink");
        createLink.disableProperty().bind(working);
        createLink.setOnAction(e -> onCreateAccount.run());
        HBox createRow = new HBox(6, Theme.text("New here?", "pnp-muted"), createLink);
        createRow.setAlignment(Pos.CENTER_LEFT);

        root = AuthLayout.page("Log in", "Welcome back! Log in to see your meal plan.",
                AuthLayout.labeled("Username", usernameField),
                AuthLayout.labeled("Password", passwordField), forgotRow,
                loginButton, messageLabel, createRow);

        loginButton.setOnAction(e -> handleLogin());
        usernameField.setOnAction(e -> {
            e.consume(); // Enter in the username box only moves to the password box
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
                return auth.login(username, password);   // PBKDF2 hashing is slow on purpose
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
            Theme.status(messageLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }
}
