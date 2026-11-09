package com.platenplatter.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The shared look of the Log in and Sign up pages: the cream page with one white card in the middle,
 * the maroon "Plate N Platter" title on top. Same colors and fonts as the rest of the app (Theme).
 */
final class AuthLayout {
    private AuthLayout() { }

    /** A full page: the brand, a heading, a short line under it, then the form. */
    static Parent page(String heading, String subheading, Node... body) {
        Label brand = new Label("Plate N Platter");
        brand.getStyleClass().add("pnp-title");
        Label head = new Label(heading);
        head.getStyleClass().add("pnp-section");
        head.setStyle("-fx-font-size: 17px;");
        Label sub = new Label(subheading);
        sub.getStyleClass().add("pnp-muted");
        sub.setWrapText(true);

        VBox card = new VBox(12);
        card.getChildren().addAll(brand, head, sub);
        card.getChildren().addAll(body);
        card.getStyleClass().add("pnp-card");
        card.setPadding(new Insets(28));
        card.setMaxWidth(430);
        card.setMaxHeight(Region.USE_PREF_SIZE);

        StackPane root = new StackPane(card);
        root.setPadding(new Insets(24));
        StackPane.setAlignment(card, Pos.CENTER);
        Theme.apply(root);
        return root;
    }

    /** A small grey label that sits above a field. */
    static Label fieldLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("pnp-muted");
        label.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        return label;
    }

    /** A field with its label above it. */
    static VBox labeled(String text, Node field) {
        return new VBox(4, fieldLabel(text), field);
    }

    /** A button that looks like an underlined link (for "Create an account" and "Back to log in"). */
    static Button link(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("pnp-link");
        return b;
    }
}
