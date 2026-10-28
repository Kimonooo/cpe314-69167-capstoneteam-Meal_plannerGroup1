package com.platenplatter.ui;

import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import com.platenplatter.util.PasswordHasher;
import java.sql.SQLException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** The sign-up form. Day 2: a valid form is saved to the database with a salted password hash. */
public class RegisterScreen {
    private final UserDao userDao = new UserDao();
    private final TextField nameField = new TextField();
    private final TextField usernameField = new TextField();
    private final TextField ageField = new TextField();
    private final ComboBox<String> genderBox = new ComboBox<>();
    private final PasswordField passwordField = new PasswordField();
    private final PasswordField confirmField = new PasswordField();
    private final Button createButton = new Button("Create Account");
    private final Label messageLabel = new Label();

    public RegisterScreen() {
        genderBox.getItems().addAll("F", "M", "Other");
        // This is the event handler: it runs every time the button is clicked
        createButton.setOnAction(event -> handleCreateAccount());
    }

    public Parent getView() {
        Label title = new Label("Plate N Platter - Sign up");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5A000A;");

        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        form.addRow(0, new Label("Name:"), nameField);
        form.addRow(1, new Label("Username:"), usernameField);
        form.addRow(2, new Label("Age:"), ageField);
        form.addRow(3, new Label("Gender:"), genderBox);
        form.addRow(4, new Label("Password:"), passwordField);
        form.addRow(5, new Label("Confirm password:"), confirmField);

        createButton.setStyle("-fx-background-color: #5A000A; -fx-text-fill: white;");

        VBox root = new VBox(15, title, form, createButton, messageLabel);
        root.setPadding(new Insets(30));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FFFBD0;");
        return root;
    }

    /** Checks the form, then saves the new user. Shows a message for every possible problem. */
    private void handleCreateAccount() {
        String name = nameField.getText().trim();
        String username = usernameField.getText().trim();
        String ageText = ageField.getText().trim();
        String gender = genderBox.getValue();
        String password = passwordField.getText();
        String confirm = confirmField.getText();

        if (name.isEmpty() || username.isEmpty() || ageText.isEmpty()
                || gender == null || password.isEmpty()) {
            showError("Please fill in every field.");
            return;
        }
        int age;
        try {
            age = Integer.parseInt(ageText);
        } catch (NumberFormatException e) {
            showError("Age must be a number.");
            return;
        }
        if (age < 1 || age > 120) {
            showError("Age must be between 1 and 120.");
            return;
        }
        if (password.length() < 6) {
            showError("Password must be at least 6 characters.");
            return;
        }
        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            if (userDao.findByUsername(username) != null) {
                showError("That username is already taken. Please choose another.");
                return;
            }
            String salt = PasswordHasher.generateSalt();
            String hash = PasswordHasher.hash(password, salt); // the real password is never saved
            userDao.insertUser(new User(0, name, username, age, gender, hash, salt));
            showSuccess("Account created! Welcome, " + name + ".");
            clearForm();
        } catch (SQLException e) {
            showError("Database problem: " + e.getMessage());
        }
    }

    private void showError(String text) {
        messageLabel.setStyle("-fx-text-fill: #B00020;");
        messageLabel.setText(text);
    }

    private void showSuccess(String text) {
        messageLabel.setStyle("-fx-text-fill: #1B5E20;");
        messageLabel.setText(text);
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
