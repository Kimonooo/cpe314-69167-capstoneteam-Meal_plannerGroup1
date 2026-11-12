package com.platenplatter.ui;

import com.platenplatter.service.AuthException;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The shared look of the Log in, Sign up and Forgot Password pages: the logo on a dark card. */
final class AuthLayout {
    private AuthLayout() { }

    static Parent page(String heading, String subheading, Node... body) {
        Label head = Theme.text(heading, "pnp-title");
        Label sub = Theme.text(subheading, "pnp-subtitle");
        sub.setWrapText(true);
        VBox brand = new VBox(Theme.logo(92));
        brand.setAlignment(Pos.CENTER);
        VBox titles = new VBox(4, head, sub);
        VBox card = new VBox(16, brand, titles);
        card.getChildren().addAll(body);
        card.getStyleClass().addAll("pnp-card", "pnp-auth-card");
        card.setMaxWidth(440);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane root = new StackPane(card);
        root.setPadding(new Insets(24));
        StackPane.setAlignment(card, Pos.CENTER);
        Theme.apply(root);
        root.getStyleClass().add("pnp-auth");
        return root;
    }

    static VBox labeled(String text, Node field) {
        Label label = Theme.text(text, "pnp-muted");
        label.getStyleClass().add("pnp-field-label");
        return new VBox(4, label, field);
    }

    static Button link(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("pnp-link");
        return button;
    }

    /** The text of an error: our own rule messages as they are, anything else as a database problem. */
    static String errorText(Throwable error) {
        return error instanceof AuthException ? error.getMessage()
                : "Database problem: " + (error == null ? "unknown error" : error.getMessage());
    }
}
