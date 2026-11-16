package com.platenplatter.ui;

import com.platenplatter.controller.UserMessages;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
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
        VBox brand = new VBox(Theme.logo(84));
        brand.setAlignment(Pos.CENTER);
        VBox titles = new VBox(4, head, sub);
        VBox card = new VBox(14, brand, titles);
        card.getChildren().addAll(body);
        card.getStyleClass().addAll("pnp-card", "pnp-auth-card");
        card.setMaxWidth(460);
        card.setMaxHeight(Region.USE_PREF_SIZE);
        StackPane centered = new StackPane(card);
        centered.setPadding(new Insets(24));
        StackPane.setAlignment(card, Pos.CENTER);
        ScrollPane scroll = new ScrollPane(centered);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.getStyleClass().addAll("pnp-scroll", "pnp-auth");
        centered.getStyleClass().add("pnp-auth");
        Theme.apply(scroll);
        return scroll;
    }

    /** A bold label above a box; the label is tied to the box for screen readers. */
    static VBox labeled(String text, Node field) {
        return Theme.labeled(text, field);
    }

    static Button link(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("pnp-link");
        return button;
    }

    /**
     * A secret box (the security answer) that is hidden like a password, with a "Show answer"
     * tick to check what was typed. The hidden box and the plain box share one text.
     */
    static VBox secret(PasswordField hidden, String showText) {
        TextField plain = new TextField();
        plain.setId(hidden.getId() + "Plain");
        plain.getStyleClass().addAll(hidden.getStyleClass());
        plain.promptTextProperty().bind(hidden.promptTextProperty());
        plain.textProperty().bindBidirectional(hidden.textProperty());
        Theme.show(plain, false);
        CheckBox show = new CheckBox(showText);
        show.setId(hidden.getId() + "Show");
        show.getStyleClass().addAll("pnp-check", "pnp-hint");
        show.selectedProperty().addListener((obs, was, isShown) -> {
            Theme.show(plain, isShown);
            Theme.show(hidden, !isShown);
        });
        return new VBox(6, new StackPane(hidden, plain), show);
    }

    /**
     * The sentence for an error: our own rule messages as they are, anything else in plain words.
     */
    static String errorText(Throwable error) {
        return UserMessages.of(error);
    }
}
