package com.platenplatter.ui;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import java.net.URL;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * The look of the app in one place: loads platenplatter.css, makes the colored chips,
 * colors the status lines and shows the team logo.
 * Brand colors (from the logo): maroon #6B0E2D, cream #F6F0E4, brown #764640.
 */
public final class Theme {
    private static final String CSS = "/com/platenplatter/ui/platenplatter.css";
    private static final String LOGO = "/com/platenplatter/ui/logo.png";

    /** The kind of message a status line shows: grey, green or red. */
    public enum Kind { INFO, OK, ERROR }

    private Theme() { }

    /** Adds the stylesheet and the base style to a page. */
    public static void apply(Parent root) {
        URL url = Theme.class.getResource(CSS);
        if (url != null && !root.getStylesheets().contains(url.toExternalForm())) {
            root.getStylesheets().add(url.toExternalForm());
        }
        addClass(root, "pnp-root");
    }

    /** The team logo at the given height, or an empty view if the picture is missing. */
    public static ImageView logo(double height) {
        URL url = Theme.class.getResource(LOGO);
        ImageView view = url == null ? new ImageView() : new ImageView(new Image(url.toExternalForm()));
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    public static Label chip(String text, String variantClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("pnp-chip", variantClass);
        return chip;
    }

    public static Label healthChip(HealthyLevel level) {
        return chip(level == null ? "?" : level.getLabel(), level == null ? "chip-neutral" : "chip-" + level.name().toLowerCase());
    }

    public static Label slotChip(MealSlot slot) {
        return chip(slot == null ? "?" : slot.getLabel(), "chip-slot");
    }

    public static Label tierChip(PrepTier tier) {
        return chip(tier == null ? "?" : tier.getLabel(), tier == null ? "chip-neutral" : "chip-" + tier.name().toLowerCase());
    }

    /** A label with a style class, the way most screens make their text. */
    public static Label text(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    /** The big title and grey line at the top of a page. */
    public static VBox pageHeader(String title, String subtitle) {
        Label sub = text(subtitle, "pnp-subtitle");
        sub.setWrapText(true);
        return new VBox(2, text(title, "pnp-title"), sub);
    }

    /** A white card with a small caption, a big number and a grey unit line, sharing a row equally. */
    public static VBox statCard(String caption, Label value, Label unit) {
        value.getStyleClass().add("pnp-stat-value");
        unit.getStyleClass().add("pnp-muted");
        VBox card = new VBox(2, text(caption, "pnp-stat-label"), value, unit);
        card.getStyleClass().add("pnp-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(10);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    /** Shows a message in a status line, colored by its kind. */
    public static void status(Label label, String text, Kind kind) {
        label.setText(text);
        label.getStyleClass().removeAll("ok", "error", "info");
        label.getStyleClass().add(kind.name().toLowerCase());
        addClass(label, "pnp-status");
    }

    public static void addClass(Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }

    /** Shows or hides a node, and frees its space when hidden. */
    public static void show(Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
