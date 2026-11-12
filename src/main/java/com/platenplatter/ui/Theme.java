package com.platenplatter.ui;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PrepTier;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javafx.animation.Animation;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.layout.VBox;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;

/**
 * The look of the app in one place: loads platenplatter.css, makes the colored chips,
 * colors the status lines and shows the team logo.
 * Dark burgundy theme: background #1A0F14, cards #311B24, accent rose #B85C7D, text off-white #F6EDE6,
 * titles soft gold #EAD9B8. Every color lives at the top of platenplatter.css.
 * It also loads the small picture assets (chef hat, food, dishes, meal, streak, trophy ...) from the icons folder.
 */
public final class Theme {
    private static final String CSS = "/com/platenplatter/ui/platenplatter.css";
    private static final String LOGO = "/com/platenplatter/ui/logo.png";
    private static final String ICONS = "/com/platenplatter/ui/icons/";
    private static final Map<String, Image> ICON_CACHE = new HashMap<String, Image>();   // each picture is read once
    private static final Duration MOTION = Duration.millis(140);
    private static final double SINK = 0.08;                   // how much of the wave picture sinks below the banner edge

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
        installMotion(root);
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

    /** A small picture from the icons folder (for example "streak" or "trophy") at the given height; empty if missing. */
    public static ImageView icon(String name, double height) {
        ImageView view = new ImageView();
        swapIcon(view, name);
        view.setFitHeight(height);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        return view;
    }

    /** Shows another icons-folder picture in an existing view (the banner changes its picture for each page). */
    public static void swapIcon(ImageView view, String name) {
        Image image = ICON_CACHE.get(name);
        if (image == null) {
            URL url = Theme.class.getResource(ICONS + name + ".png");
            if (url == null) {
                return;
            }
            image = new Image(url.toExternalForm());
            ICON_CACHE.put(name, image);
        }
        view.setImage(image);
    }

    /** A page's section heading with a small picture before it. */
    public static Label sectionTitle(String text, String iconName) {
        Label label = text(text, "pnp-section");
        label.setGraphic(icon(iconName, 24));
        label.setGraphicTextGap(10);
        return label;
    }

    /** A friendly empty state: a faded picture with a centered message under it. */
    public static VBox emptyState(String text, String iconName) {
        ImageView picture = icon(iconName, 64);
        picture.setOpacity(0.55);
        Label label = text(text, "pnp-empty");
        label.setWrapText(true);
        label.setTextAlignment(TextAlignment.CENTER);
        VBox box = new VBox(8, picture, label);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(14));
        return box;
    }

    /** A faint, mouse-transparent picture meant to sit on the right side of a StackPane (see withWatermark). */
    public static ImageView watermark(String iconName, double height, double opacity, double rightMargin) {
        ImageView mark = icon(iconName, height);
        mark.setOpacity(opacity);
        mark.setMouseTransparent(true);
        StackPane.setAlignment(mark, Pos.CENTER_RIGHT);
        StackPane.setMargin(mark, new Insets(0, rightMargin, 0, 0));
        return mark;
    }

    /**
     * Puts a faint picture behind a node, on its right side (the chef hat in the banner, the botanical
     * drawing on the featured meal). The picture ignores the mouse, so buttons on top stay clickable.
     */
    public static StackPane withWatermark(Node content, String iconName, double height, double opacity, double rightMargin) {
        StackPane stack = new StackPane(watermark(iconName, height, opacity, rightMargin), content);
        stack.setAlignment(Pos.CENTER_LEFT);
        return stack;
    }

    /**
     * The wave picture along the bottom of a banner. It comes in two pieces cut where only waves are
     * left: the left piece is stretched to fill the width (soft waves stay soft), and the right piece
     * (chef hat, fork and spoon) keeps its shape. Both are as high as heightShare of the banner, sink a
     * little below its bottom edge (what sticks out is clipped) and ignore the mouse. Put it first in a StackPane, under the text.
     */
    public static Pane waves(double heightShare) {
        Pane pane = new Pane();
        pane.setMouseTransparent(true);
        Rectangle clip = new Rectangle();                     // nothing may be drawn outside the banner
        clip.widthProperty().bind(pane.widthProperty());
        clip.heightProperty().bind(pane.heightProperty());
        pane.setClip(clip);
        ImageView left = icon("wave_left", 10);
        ImageView right = icon("wave_right", 10);
        if (left.getImage() == null || right.getImage() == null) {
            return pane;                                      // picture missing: the plain gradient shows
        }
        left.setPreserveRatio(false);
        left.setManaged(false);                               // the pane never grows because of the pictures
        right.setManaged(false);
        double ratio = right.getImage().getWidth() / right.getImage().getHeight();
        DoubleBinding high = pane.heightProperty().multiply(heightShare);
        DoubleBinding rightWidth = high.multiply(ratio);
        left.fitHeightProperty().bind(high);
        right.fitHeightProperty().bind(high);
        left.fitWidthProperty().bind(Bindings.createDoubleBinding(
                () -> Math.max(0, pane.getWidth() - rightWidth.get()), pane.widthProperty(), rightWidth));
        DoubleBinding top = pane.heightProperty().subtract(high.multiply(1.0 - SINK));
        left.layoutYProperty().bind(top);
        right.layoutYProperty().bind(top);
        right.layoutXProperty().bind(pane.widthProperty().subtract(rightWidth));
        pane.getChildren().addAll(left, right);
        return pane;
    }

    // ------------------------------------------------------------------ button motion

    /**
     * Every button grows a little when the mouse is on it and shrinks a little while it is pressed; a
     * recipe card also lifts. JavaFX CSS has no transitions, so this is done with short animations.
     * One handler set on a page's root covers every button on that page, including ones added later.
     */
    private static void installMotion(Parent root) {
        if (root.getProperties().putIfAbsent("motionInstalled", Boolean.TRUE) != null) {
            return;                                           // this root already has it
        }
        root.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, e -> hover(e.getTarget(), true));
        root.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, e -> hover(e.getTarget(), false));
        root.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> press(e.getTarget(), true));
        root.addEventHandler(MouseEvent.MOUSE_RELEASED, e -> press(e.getTarget(), false));
    }

    /** The button (or recipe card) that the mouse event happened on, or null when it should stay still. */
    private static Node animated(Object target) {
        Node node = target instanceof Node ? (Node) target : null;
        while (node != null) {
            boolean ours = node.getStyleClass().stream().anyMatch(name -> name.startsWith("pnp-"));
            if (ours && node.getStyleClass().contains("pnp-recipe-card")) {
                return node;
            }
            if (ours && node instanceof ButtonBase && !(node instanceof CheckBox) && !node.isDisabled()
                    && !node.getStyleClass().contains("pnp-link")) {
                return node;
            }
            node = node.getParent();
        }
        return null;
    }

    private static void hover(Object target, boolean inside) {
        Node node = animated(target);
        if (node == null) {
            return;
        }
        if (node.getStyleClass().contains("pnp-recipe-card")) {
            play(node, inside ? 1.015 : 1.0, inside ? -4 : 0, true);
        } else {
            play(node, inside ? 1.05 : 1.0, 0, false);
        }
    }

    private static void press(Object target, boolean down) {
        Node node = animated(target);
        if (node == null || node.getStyleClass().contains("pnp-recipe-card")) {
            return;
        }
        play(node, down ? 0.95 : (node.isHover() ? 1.05 : 1.0), 0, false);
    }

    private static void play(Node node, double scale, double lift, boolean moves) {
        Object running = node.getProperties().get("motionAnimation");
        if (running instanceof Animation) {
            ((Animation) running).stop();                     // start from where the last one stopped
        }
        ScaleTransition grow = new ScaleTransition(MOTION, node);
        grow.setToX(scale);
        grow.setToY(scale);
        grow.setInterpolator(Interpolator.EASE_OUT);
        ParallelTransition both = new ParallelTransition(grow);
        if (moves) {
            TranslateTransition up = new TranslateTransition(MOTION, node);
            up.setToY(lift);
            up.setInterpolator(Interpolator.EASE_OUT);
            both.getChildren().add(up);
        }
        node.getProperties().put("motionAnimation", both);
        both.play();
    }

    /** A card with a small caption, a big number and a muted unit line, sharing a row equally. */
    public static VBox statCard(String caption, Label value, Label unit) {
        return statCard(caption, value, unit, null);
    }

    /** The same card with a small picture on its right (the trophy on Best Streak, the dishes on Meals Today). */
    public static VBox statCard(String caption, Label value, Label unit, String iconName) {
        value.getStyleClass().add("pnp-stat-value");
        unit.getStyleClass().add("pnp-muted");
        VBox words = new VBox(2, text(caption, "pnp-stat-label"), value, unit);
        HBox.setHgrow(words, Priority.ALWAYS);
        VBox card = new VBox();
        if (iconName == null) {
            card.getChildren().add(words);
        } else {
            ImageView picture = icon(iconName, 56);
            HBox row = new HBox(10, words, picture);
            row.setAlignment(Pos.CENTER_LEFT);
            card.getChildren().add(row);
        }
        card.getStyleClass().addAll("pnp-card", "pnp-stat-card");
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
