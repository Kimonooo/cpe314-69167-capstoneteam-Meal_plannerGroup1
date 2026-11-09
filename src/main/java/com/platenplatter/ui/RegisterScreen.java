package com.platenplatter.ui;

import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthException;
import com.platenplatter.service.AuthService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

/**
 * The sign-up form. A valid form is saved to the database with a salted password hash.
 * The RULES (what is a valid username, age, password) live in AuthService, so they can be tested;
 * this screen only reads the fields and shows the message. Saving runs in a background Task.
 * Controls that work: every field, Create Account, Back to log in.
 */
public class RegisterScreen {
    private final AuthService auth;
    private final Consumer<String> onRegistered;

    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final TextField ageField = new TextField();
    private final ComboBox<String> genderBox = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Button createButton = new Button("Create Account");
    private final Button backLink = AuthLayout.link("Back to log in");
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    /** The Milestone 1 App uses this: the form stays open and shows "Account created!". */
    public RegisterScreen() {
        this(new AuthService(new UserDao()), null, null);
    }

    /**
     * @param onRegistered gets the new username after a successful sign-up (null = stay on the form)
     * @param onBack       what "Back to log in" does (null = the link is hidden)
     */
    public RegisterScreen(AuthService auth, Consumer<String> onRegistered, Runnable onBack) {
        this.auth = auth;
        this.onRegistered = onRegistered;

        genderBox.getItems().addAll("F", "M", "Other");
        genderBox.setPromptText("Choose");
        genderBox.setMaxWidth(Double.MAX_VALUE);
        nameField.setId("registerName");
        nameField.setPromptText("The name shown in the app, e.g. Jon");
        usernameField.setId("registerUsername");
        usernameField.setPromptText("3 to 20 letters or numbers, no spaces");
        ageField.setId("registerAge");
        ageField.setPromptText("e.g. 20");
        passwordField.setId("registerPassword");
        passwordField.setPromptText("At least " + AuthService.MIN_PASSWORD_LENGTH + " characters");
        confirmField.setId("registerConfirm");
        confirmField.setPromptText("Type it again");
        for (TextField f : new TextField[] {nameField, usernameField, ageField, passwordField, confirmField}) {
            f.getStyleClass().add("pnp-field");
        }

        createButton.setId("createButton");
        createButton.getStyleClass().add("pnp-button");
        createButton.setDefaultButton(true);
        createButton.disableProperty().bind(working);
        createButton.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(createButton, Priority.ALWAYS);
        // This is the event handler: it runs every time the button is clicked
        createButton.setOnAction(event -> handleCreateAccount());

        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(22, 22);
        busy.setMaxSize(22, 22);
        busy.visibleProperty().bind(working);
        busy.managedProperty().bind(working);
        HBox buttonRow = new HBox(10, createButton, busy);
        buttonRow.setAlignment(Pos.CENTER_LEFT);

        messageLabel.setId("registerMessage");
        messageLabel.setWrapText(true);

        HBox ageGender = new HBox(10, AuthLayout.labeled("Age", ageField), AuthLayout.labeled("Gender", genderBox));
        HBox.setHgrow(ageGender.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(ageGender.getChildren().get(1), Priority.ALWAYS);

        backLink.setId("backToLogin");
        backLink.disableProperty().bind(working);
        Label have = new Label("Already have an account?");
        have.getStyleClass().add("pnp-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox backRow = new HBox(6, have, backLink, spacer);
        backRow.setAlignment(Pos.CENTER_LEFT);
        if (onBack == null) {
            backRow.setVisible(false);
            backRow.setManaged(false);
        } else {
            backLink.setOnAction(e -> onBack.run());
        }

        root = AuthLayout.page("Create your account", "You can change your display name later on the Account tab.",
                AuthLayout.labeled("Display name", nameField),
                AuthLayout.labeled("Username", usernameField),
                ageGender,
                AuthLayout.labeled("Password", passwordField),
                AuthLayout.labeled("Confirm password", confirmField),
                buttonRow, messageLabel, backRow);
    }

    public Parent getView() {
        return root;
    }

    /** Sends the form to AuthService (in the background) and shows the result. */
    private void handleCreateAccount() {
        String name = nameField.getText();
        String username = usernameField.getText();
        String age = ageField.getText();
        String gender = genderBox.getValue();
        String password = passwordField.getText();
        String confirm = confirmField.getText();
        working.set(true);
        Theme.status(messageLabel, "Creating your account...", Theme.Kind.INFO);

        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.register(name, username, age, gender, password, confirm);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            User user = task.getValue();
            if (onRegistered != null) {
                onRegistered.accept(user.getUsername());
            } else {
                Theme.status(messageLabel, "Account created! Welcome, " + user.getName() + ".", Theme.Kind.OK);
                clearForm();
            }
        });
        task.setOnFailed(e -> {
            working.set(false);
            Throwable ex = task.getException();
            String text = ex instanceof AuthException ? ex.getMessage()
                    : "Database problem: " + (ex == null ? "unknown error" : ex.getMessage());
            Theme.status(messageLabel, text, Theme.Kind.ERROR);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void clearForm() {
        nameField.clear();
        usernameField.clear();
        ageField.clear();
        genderBox.setValue(null);
        passwordField.clear();
        confirmField.clear();
    }
}
