package com.platenplatter.ui;

import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The Account page: change the display name, see the account details, log out, or delete the
 * account. Deleting asks for the password and a tick first, because it cannot be undone.
 */
public class AccountScreen {
    private final AuthService auth;
    private final Consumer<User> onRenamed;
    private Runnable onDeleted = () -> { };
    private User user;
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final PasswordField deletePassword = new PasswordField();
    private final CheckBox deleteConfirm = new CheckBox("I understand that this cannot be undone.");
    private final Button deleteButton = new Button("Delete my account");
    private final TextField nameField = new TextField();
    private final Button saveButton = new Button("Save name");
    private final Label statusLabel = new Label();
    private final BooleanProperty working = new SimpleBooleanProperty(false);

    public AccountScreen(AuthService auth, User user, Consumer<User> onRenamed, Runnable onLogout) {
        this.auth = auth;
        this.user = user;
        this.onRenamed = onRenamed;
        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(
                buildNameCard(), buildDetailsCard(onLogout), buildDeleteCard(), statusLabel);
        statusLabel.setId("accountStatus");
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        Theme.status(statusLabel, "Your display name is the name in the greeting at the top.", Theme.Kind.INFO);
    }

    public Parent getView() {
        return scroller;
    }

    /** What to do after the account was deleted (the app goes back to Log in). */
    public void setOnDeleted(Runnable onDeleted) {
        this.onDeleted = onDeleted;
    }

    private VBox buildNameCard() {
        nameField.setId("displayNameField");
        nameField.setText(user.getDisplayName());
        nameField.setPromptText("For example Kim");
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
        VBox card = new VBox(10, Theme.sectionTitle("Display name", "hat"), row,
                Theme.text("1 to " + AuthService.MAX_NAME_LENGTH + " characters. Your username does not change.", "pnp-muted"));
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildDetailsCard(Runnable onLogout) {
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(8);
        Label username = new Label(user.getUsername());
        username.setId("accountUsername");
        grid.addRow(0, label("Username"), username);
        grid.addRow(1, label("Age"), new Label(user.getAge() > 0 ? String.valueOf(user.getAge()) : "-"));
        grid.addRow(2, label("Gender"), new Label(blankToDash(user.getGender())));
        grid.addRow(3, label("Security question"), new Label(blankToDash(user.getSecurityQuestion())));
        Button logout = new Button("Log out");
        logout.setId("logoutButton");
        logout.getStyleClass().add("pnp-button-secondary");
        logout.disableProperty().bind(working);
        logout.setOnAction(e -> onLogout.run());
        VBox card = new VBox(12, Theme.sectionTitle("Account details", "chef_hat"), grid, logout);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    /** The red card at the bottom: what is removed, the password box, the tick and the button. */
    private VBox buildDeleteCard() {
        Label what = Theme.text("This permanently removes your account, your saved recipes with their ingredient lists, "
                + "and every meal you planned or imported. The streak and the charts are made from those meals, so they "
                + "go too. Other users are not touched.", "pnp-muted");
        what.setWrapText(true);
        deletePassword.setId("deletePasswordField");
        deletePassword.setPromptText("Type your password to confirm");
        deletePassword.getStyleClass().add("pnp-field");
        deletePassword.setMaxWidth(360);
        deleteConfirm.setId("deleteConfirmBox");
        deleteConfirm.getStyleClass().add("pnp-check");
        deleteButton.setId("deleteAccountButton");
        deleteButton.getStyleClass().add("pnp-button-danger");
        deleteButton.disableProperty().bind(working.or(deleteConfirm.selectedProperty().not()));
        deleteButton.setOnAction(e -> deleteAccount());
        VBox card = new VBox(10, Theme.text("Delete my account", "pnp-section"), what, deletePassword, deleteConfirm,
                deleteButton);
        card.getStyleClass().addAll("pnp-card", "pnp-danger-card");
        return card;
    }

    private void deleteAccount() {
        String password = deletePassword.getText();
        User current = user;
        working.set(true);
        Theme.status(statusLabel, "Deleting your account...", Theme.Kind.INFO);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                auth.deleteAccount(current, password);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            onDeleted.run();                              // back to Log in
        });
        task.setOnFailed(e -> {
            working.set(false);
            deletePassword.clear();
            Theme.status(statusLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    private static Label label(String text) {
        Label label = Theme.text(text, "pnp-muted");
        label.setStyle("-fx-font-weight: bold;");
        return label;
    }

    private static String blankToDash(String text) {
        return text == null || text.isBlank() ? "-" : text;
    }

    private void saveName() {
        String newName = nameField.getText();
        User current = user;
        working.set(true);
        Theme.status(statusLabel, "Saving...", Theme.Kind.INFO);
        Task<User> task = new Task<>() {
            @Override
            protected User call() throws Exception {
                return auth.rename(current, newName);
            }
        };
        task.setOnSucceeded(e -> {
            working.set(false);
            user = task.getValue();
            nameField.setText(user.getDisplayName());
            onRenamed.accept(user);
            Theme.status(statusLabel, "Saved! We will call you " + user.getDisplayName() + " from now on.", Theme.Kind.OK);
        });
        task.setOnFailed(e -> {
            working.set(false);
            Theme.status(statusLabel, AuthLayout.errorText(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }
}
