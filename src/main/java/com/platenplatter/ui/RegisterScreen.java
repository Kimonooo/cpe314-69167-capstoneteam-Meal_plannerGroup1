package com.platenplatter.ui;

import com.platenplatter.controller.AuthController;
import com.platenplatter.controller.AuthController.SignUpForm;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Pos;
import javafx.geometry.VPos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * Sign up: the account details, a password, and a security question for Forgot password.
 * Age and Gender, and Password and Confirm, sit in ONE grid with two equal columns, so the two
 * boxes of a row always have the same width and height. The answer is hidden like a password,
 * with a "Show answer" tick.
 */
public class RegisterScreen {
    /** The words shown and saved for gender. */
    static final String[] GENDERS = {"Female", "Male", "Other"};

    private final AuthController auth;
    private final Consumer<String> onRegistered;
    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final TextField ageField = new TextField();
    private final ComboBox<String> genderBox = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final ComboBox<String> questionBox = new ComboBox<>();
    private final PasswordField answerField = new PasswordField();
    private final Button createButton = new Button("Create account");
    private final Label messageLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);
    private final Parent root;

    public RegisterScreen(AuthController auth, Consumer<String> onRegistered, Runnable onBack) {
        this.auth = auth;
        this.onRegistered = onRegistered;
        setUpInputs();
        createButton.setId("createButton");
        createButton.getStyleClass().add("pnp-button");
        createButton.setDefaultButton(true);
        createButton.setMaxWidth(Double.MAX_VALUE);
        createButton.disableProperty().bind(working);
        createButton.setOnAction(e -> handleCreateAccount());
        messageLabel.setId("registerMessage");
        messageLabel.setWrapText(true);

        Button backLink = AuthLayout.link("Back to log in");
        backLink.setId("backToLogin");
        backLink.disableProperty().bind(working);
        backLink.setOnAction(e -> onBack.run());
        HBox backRow = new HBox(6, Theme.text("Already have an account?", "pnp-muted"), backLink);
        backRow.setAlignment(Pos.CENTER_LEFT);

        root = AuthLayout.page("Create your account",
                "The security question lets you reset a forgotten password.",
                AuthLayout.labeled("Display name", nameField),
                AuthLayout.labeled("Username", usernameField),
                twoColumns(AuthLayout.labeled("Age", ageField), AuthLayout.labeled("Gender",
                        genderBox),
                        AuthLayout.labeled("Password", passwordField),
                        AuthLayout.labeled("Confirm password", confirmField)),
                AuthLayout.labeled("Security question", questionBox),
                AuthLayout.labeled("Answer", AuthLayout.secret(answerField, "Show answer")),
                createButton, messageLabel, backRow);
    }

    /** Ids, hints and widths of the boxes; the security questions come from AuthController. */
    private void setUpInputs() {
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
        answerField.setPromptText("Upper or lower case does not matter");
        Stream.of(nameField, usernameField, ageField, passwordField, confirmField, answerField)
                .forEach(field -> field.getStyleClass().add("pnp-field"));
        genderBox.setId("registerGender");
        genderBox.getItems().addAll(GENDERS);
        genderBox.setPromptText("Choose");
        Stream.of(genderBox, questionBox, ageField, passwordField, confirmField)
                .forEach(box -> box.setMaxWidth(Double.MAX_VALUE));
        questionBox.setId("registerQuestion");
        questionBox.getItems().addAll(auth.securityQuestions());
        questionBox.setPromptText("Pick a question you will remember");
    }

    public Parent getView() {
        return root;
    }

    /** Two rows of two boxes in one grid: both columns are always 50 percent wide. */
    private static GridPane twoColumns(Node topLeft, Node topRight, Node bottomLeft,
            Node bottomRight) {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(12);
        for (int i = 0; i < 2; i++) {
            ColumnConstraints half = new ColumnConstraints();
            half.setPercentWidth(50);
            half.setHgrow(Priority.ALWAYS);
            half.setFillWidth(true);
            grid.getColumnConstraints().add(half);
        }
        grid.addRow(0, topLeft, topRight);
        grid.addRow(1, bottomLeft, bottomRight);
        Stream.of(topLeft, topRight, bottomLeft, bottomRight).forEach(cell -> {
            GridPane.setValignment(cell, VPos.TOP);
            if (cell instanceof Region region) {
                region.setMaxWidth(Double.MAX_VALUE);
            }
        });
        return grid;
    }

    private void handleCreateAccount() {
        SignUpForm form = new SignUpForm(nameField.getText(), usernameField.getText(),
                ageField.getText(), genderBox.getValue(), passwordField.getText(),
                        confirmField.getText(),
                questionBox.getValue(), answerField.getText());
        working.set(true);
        Theme.status(messageLabel, "Creating your account...", Theme.Kind.INFO);
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.signUp(form);
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
