package com.platenplatter.ui;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import javafx.scene.Parent;
import javafx.scene.control.Label;

import java.net.URL;

/**
 * Shared look for all tabs (cream background, maroon buttons, white cards).
 * Every screen uses these helpers, so a color is changed in ONE place (here and platenplatter.css).
 * Healthy level colors are the same everywhere: Healthy = green, Moderate = yellow, Indulgent = red.
 */
public final class Theme {
    private static final String CSS = "/com/platenplatter/ui/platenplatter.css";

    /** The three kinds of message shown on a status line. */
    public enum Kind { INFO, OK, ERROR }

    private Theme() { }

    /** Loads the stylesheet onto a screen. Safe to call twice; never crashes if the file is missing. */
    public static void apply(Parent root) {
        URL url = Theme.class.getResource(CSS);
        if (url != null) {
            String css = url.toExternalForm();
            if (!root.getStylesheets().contains(css)) {
                root.getStylesheets().add(css);
            }
        }
        addClass(root, "pnp-root");
    }

    /** The colored chip style for one of our three plan categories. */
    public static String chipClass(String planCategory) {
        if (planCategory == null) {
            return "chip-neutral";
        }
        switch (planCategory) {
            case "Breakfast": return "chip-breakfast";
            case "MainCourse": return "chip-main";
            case "Dessert": return "chip-dessert";
            default: return "chip-neutral";
        }
    }

    /** A small rounded label, for example the category chip. */
    public static Label chip(String text, String variantClass) {
        Label chip = new Label(text);
        chip.getStyleClass().addAll("pnp-chip", variantClass);
        return chip;
    }

    /** The chip style of a healthy level: chip-healthy (green), chip-moderate (yellow), chip-indulgent (red). */
    public static String healthClass(HealthyLevel level) {
        return level == null ? "chip-neutral" : "chip-" + level.name().toLowerCase();
    }

    /** A green / yellow / red chip that says Healthy, Moderate or Indulgent. */
    public static Label healthChip(HealthyLevel level) {
        return chip(level == null ? "?" : level.getLabel(), healthClass(level));
    }

    /** A chip that says Breakfast, Lunch, Dinner or Snack. */
    public static Label slotChip(MealSlot slot) {
        return chip(slot == null ? "?" : slot.getLabel(), "chip-slot");
    }

    /** A chip that says Quick, Average or Slow (darker = slower). */
    public static Label tierChip(PrepTier tier) {
        return chip(tier == null ? "?" : tier.getLabel(), tier == null ? "chip-neutral" : "chip-" + tier.name().toLowerCase());
    }

    /** Shows a message on a status label in the right color. */
    public static void status(Label label, String text, Kind kind) {
        label.setText(text);
        label.getStyleClass().removeAll("ok", "error", "info");
        label.getStyleClass().add(kind.name().toLowerCase());
        if (!label.getStyleClass().contains("pnp-status")) {
            label.getStyleClass().add("pnp-status");
        }
    }

    public static void addClass(javafx.scene.Node node, String styleClass) {
        if (!node.getStyleClass().contains(styleClass)) {
            node.getStyleClass().add(styleClass);
        }
    }
}
