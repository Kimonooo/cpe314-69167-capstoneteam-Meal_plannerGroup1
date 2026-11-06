package com.platenplatter.ui;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthException;
import com.platenplatter.service.AuthService;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.function.Consumer;

/**
 * The "Account" tab: change the display name (shown in the header greeting) and log out.
 * The new name is saved to the users table by AuthService, in a background Task.
 * Controls that work: the name field (Enter saves), Save name, Log out.
 */
public class AccountScreen {
    private final AuthService auth;
    private final Consumer<User> onRenamed;
    private User user;

    private final VBox root = new VBox(14);
    private final TextField nameField = new TextField();
    private final Button saveButton = new Button("Save name");
    private final Button logoutButton = new Button("Log out");
    private final Label statusLabel = new Label();
    private final Label usernameValue = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);

    /**
     * @param auth      null in the previews: the name then only changes for this session
     * @param onRenamed gets the user with the new name (the header uses it)
     * @param onLogout  what Log out does (null = the button is hidden)
     */
    public AccountScreen(AuthService auth, User user, Consumer<User> onRenamed, Runnable onLogout) {
        this.auth = auth;
        this.user = user;
        this.onRenamed = onRenamed == null ? u -> { } : onRenamed;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildNameCard(), buildDetailsCard(onLogout), statusLabel);
        statusLabel.setId("accountStatus");
        Theme.apply(root);
        Theme.status(statusLabel, "Your display name is the name in the greeting at the top.", Theme.Kind.INFO);
    }

    public Parent getView() {
        return root;
    }

    private VBox buildHeader() {
        Label title = new Label("My Account");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("Change the name the app calls you, or log out.");
        subtitle.getStyleClass().add("pnp-subtitle");
        return new VBox(2, title, subtitle);
    }

    private VBox buildNameCard() {
        Label title = new Label("Display name");
        title.getStyleClass().add("pnp-section");
        nameField.setId("displayNameField");
        nameField.setText(user.getName());
        nameField.setPromptText("For example Jon");
        nameField.getStyleClass().add("pnp-field");
        nameField.setMaxWidth(360);
        HBox.setHgrow(nameField, Priority.ALWAYS);
        nameField.setOnAction(e -> saveName());

        saveButton.setId("saveNameButton");
        saveButton.getStyleClass().add("pnp-button");
        saveButton.disableProperty().bind(working);
        saveButton.setOnAction(e -> saveName());

        HBox row = new HBox(10, nameField, saveButton);
        row.setAlignment(Pos.CENTER_LEFT);
        Label hint = new Label("1 to " + AuthService.MAX_NAME_LENGTH + " characters. Your username does not change.");
        hint.getStyleClass().add("pnp-muted");
        VBox card = new VBox(10, title, row, hint);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildDetailsCard(Runnable onLogout) {
        Label title = new Label("Account details");
        title.getStyleClass().add("pnp-section");
        usernameValue.setId("accountUsername");
        usernameValue.setText(user.getUsername());

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        grid.addRow(0, muted("Username"), usernameValue);
        grid.addRow(1, muted("Age"), new Label(user.getAge() > 0 ? String.valueOf(user.getAge()) : "-"));
        grid.addRow(2, muted("Gender"), new Label(user.getGender() == null || user.getGender().isBlank() ? "-" : user.getGender()));

        logoutButton.setId("logoutButton");
        logoutButton.getStyleClass().add("pnp-button-secondary");
        logoutButton.disableProperty().bind(working);
        VBox card = new VBox(12, title, grid);
        if (onLogout != null) {
            logoutButton.setOnAction(e -> onLogout.run());
            card.getChildren().add(logoutButton);
        }
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private static Label muted(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pnp-muted");
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private void saveName() {
        String newName = nameField.getText();
        if (auth == null) { // preview: nothing is saved
            String clean = newName == null ? "" : newName.trim();
            if (clean.isEmpty()) {
                Theme.status(statusLabel, "Please enter a name.", Theme.Kind.ERROR);
                return;
            }
            user = user.withName(clean);
            onRenamed.accept(user);
            Theme.status(statusLabel, "Preview: the name changed for this session only.", Theme.Kind.OK);
            return;
        }
        working.set(true);
        Theme.status(statusLabel, "Saving...", Theme.Kind.INFO);
        User current = user;
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.rename(current, newName);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            user = task.getValue();
            nameField.setText(user.getName());
            onRenamed.accept(user);
            Theme.status(statusLabel, "Saved! We will call you " + user.getName() + " from now on.", Theme.Kind.OK);
        });
        task.setOnFailed(e -> {
            working.set(false);
            Throwable ex = task.getException();
            String text = ex instanceof AuthException ? ex.getMessage()
                    : "Could not save the name: " + (ex == null ? "unknown error" : ex.getMessage());
            Theme.status(statusLabel, text, Theme.Kind.ERROR);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
