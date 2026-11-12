package com.platenplatter.ui;

import com.platenplatter.service.AuthService;
import java.util.function.Consumer;
import java.util.stream.Stream;
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
import javafx.scene.layout.VBox;

/**
 * Forgot password, in two steps:
 *   1. type the username; the page shows that account's security question;
 *   2. type the answer and a new password twice; AuthService.resetPassword checks them and saves a new hash.
 * Both steps run in the background, like log in.
 */
public class ForgotPasswordScreen {
    private final AuthService auth;
    private final Consumer<String> onPasswordChanged;
    private final TextField usernameField = new TextField();
    private final Button findButton = new Button("Show my question");
    private final Label questionLabel = new Label();
    private final TextField answerField = new TextField();
    private final PasswordField newPasswordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Button resetButton = new Button("Change password");
    private final VBox stepTwo;
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    public ForgotPasswordScreen(AuthService auth, Consumer<String> onPasswordChanged, Runnable onBack) {
        this.auth = auth;
        this.onPasswordChanged = onPasswordChanged;
        usernameField.setId("forgotUsername");
        usernameField.setPromptText("Your username");
        answerField.setId("forgotAnswer");
        answerField.setPromptText("Your answer");
        newPasswordField.setId("forgotNewPassword");
        newPasswordField.setPromptText("At least " + AuthService.MIN_PASSWORD_LENGTH + " characters");
        confirmField.setId("forgotConfirm");
        confirmField.setPromptText("Type it again");
        Stream.of(usernameField, answerField, newPasswordField, confirmField)
                .forEach(field -> field.getStyleClass().add("pnp-field"));
        findButton.setId("findQuestionButton");
        findButton.getStyleClass().add("pnp-button-secondary");
        findButton.disableProperty().bind(working);
        findButton.setOnAction(e -> findQuestion());
        usernameField.setOnAction(e -> findQuestion());
        questionLabel.setId("forgotQuestion");
        questionLabel.setWrapText(true);
        questionLabel.getStyleClass().add("pnp-question");
        resetButton.setId("resetPasswordButton");
        resetButton.getStyleClass().add("pnp-button");
        resetButton.setMaxWidth(Double.MAX_VALUE);
        resetButton.disableProperty().bind(working);
        resetButton.setOnAction(e -> resetPassword());
        messageLabel.setId("forgotMessage");
        messageLabel.setWrapText(true);

        stepTwo = new VBox(12, questionLabel,
                AuthLayout.labeled("Answer", answerField),
                AuthLayout.labeled("New password", newPasswordField),
                AuthLayout.labeled("New password again", confirmField),
                resetButton);
        Theme.show(stepTwo, false);

        Button backLink = AuthLayout.link("Back to log in");
        backLink.setId("forgotBack");
        backLink.setOnAction(e -> onBack.run());
        HBox backRow = new HBox(6, Theme.text("Remembered it?", "pnp-muted"), backLink);
        backRow.setAlignment(Pos.CENTER_LEFT);

        root = AuthLayout.page("Forgot password", "Answer your security question to choose a new password.",
                AuthLayout.labeled("Username", usernameField), findButton, stepTwo, messageLabel, backRow);
    }

    public Parent getView() {
        return root;
    }

    /** Step 1: read the question of this username. */
    private void findQuestion() {
        String username = usernameField.getText();
        working.set(true);
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return auth.securityQuestionFor(username);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            questionLabel.setText(task.getValue());
            Theme.show(stepTwo, true);
            Theme.status(messageLabel, "Answer the question, then choose a new password.", Theme.Kind.INFO);
            answerField.requestFocus();
        });
        task.setOnFailed(e -> {
            working.set(false);
            Theme.show(stepTwo, false);
            Theme.status(messageLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    /** Step 2: check the answer and save the new password. */
    private void resetPassword() {
        String username = usernameField.getText();
        String answer = answerField.getText();
        String newPassword = newPasswordField.getText();
        String confirm = confirmField.getText();
        working.set(true);
        Theme.status(messageLabel, "Checking...", Theme.Kind.INFO);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                auth.resetPassword(username, answer, newPassword, confirm);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            onPasswordChanged.accept(username.trim());
        });
        task.setOnFailed(e -> {
            working.set(false);
            Theme.status(messageLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }
}
