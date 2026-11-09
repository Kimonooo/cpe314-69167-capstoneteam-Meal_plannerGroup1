package com.platenplatter.ui;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** Sign up: the account details, a password, and a security question for Forgot password. */
public class RegisterScreen {
    private final AuthService auth;
    private final Consumer<String> onRegistered;
    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final TextField ageField = new TextField();
    private final ComboBox<String> genderBox = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final ComboBox<String> questionBox = new ComboBox<>();
    private final TextField answerField = new TextField();
    private final Button createButton = new Button("Create Account");
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    public RegisterScreen(AuthService auth, Consumer<String> onRegistered, Runnable onBack) {
        this.auth = auth;
        this.onRegistered = onRegistered;
        nameField.setId("registerName");
        nameField.setPromptText("The name shown in the app, e.g. Kim");
        usernameField.setId("registerUsername");
        usernameField.setPromptText("3 to 20 letters or numbers, no spaces");
        ageField.setId("registerAge");
        ageField.setPromptText("e.g. 20");
        passwordField.setId("registerPassword");
        passwordField.setPromptText("At least " + AuthService.MIN_PASSWORD_LENGTH + " characters");
        confirmField.setId("registerConfirm");
        confirmField.setPromptText("Type it again");
        answerField.setId("registerAnswer");
        answerField.setPromptText("Your answer (upper/lower case does not matter)");
        Stream.of(nameField, usernameField, ageField, passwordField, confirmField, answerField)
                .forEach(field -> field.getStyleClass().add("pnp-field"));
        genderBox.getItems().addAll("F", "M", "Other");
        genderBox.setPromptText("Choose");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        questionBox.setId("registerQuestion");
        questionBox.getItems().addAll(AuthService.SECURITY_QUESTIONS);
        questionBox.setPromptText("Pick a question you will remember");
        questionBox.setMaxWidth(Double.MAX_VALUE);

        createButton.setId("createButton");
        createButton.getStyleClass().add("pnp-button");
        createButton.setDefaultButton(true);
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.disableProperty().bind(working);
        createButton.setOnAction(e -> handleCreateAccount());
        messageLabel.setId("registerMessage");
        messageLabel.setWrapText(true);

        HBox ageGender = new HBox(10, AuthLayout.labeled("Age", ageField), AuthLayout.labeled("Gender", genderBox));
        ageGender.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        HBox passwords = new HBox(10, AuthLayout.labeled("Password", passwordField),
                AuthLayout.labeled("Confirm password", confirmField));
        passwords.getChildren().forEach(node -> HBox.setHgrow(node, Priority.ALWAYS));
        Button backLink = AuthLayout.link("Back to log in");
        backLink.setId("backToLogin");
        backLink.disableProperty().bind(working);
        backLink.setOnAction(e -> onBack.run());
        HBox backRow = new HBox(6, Theme.text("Already have an account?", "pnp-muted"), backLink);
        backRow.setAlignment(Pos.CENTER_LEFT);

        root = AuthLayout.page("Create your account", "The security question lets you reset a forgotten password.",
                AuthLayout.labeled("Display name", nameField),
                AuthLayout.labeled("Username", usernameField),
                ageGender, passwords,
                AuthLayout.labeled("Security question", questionBox),
                AuthLayout.labeled("Answer", answerField),
                createButton, messageLabel, backRow);
    }

    public Parent getView() {
        return root;
    }

    private void handleCreateAccount() {
        String name = nameField.getText();
        String username = usernameField.getText();
        String age = ageField.getText();
        String gender = genderBox.getValue();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        String question = questionBox.getValue();
        String answer = answerField.getText();
        working.set(true);
        Theme.status(messageLabel, "Creating your account...", Theme.Kind.INFO);
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.register(name, username, age, gender, password, confirm, question, answer);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            onRegistered.accept(task.getValue().getUsername());
        });
        task.setOnFailed(e -> {
            working.set(false);
            Theme.status(messageLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }
}
