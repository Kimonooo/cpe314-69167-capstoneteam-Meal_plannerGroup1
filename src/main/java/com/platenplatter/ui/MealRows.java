package com.platenplatter.ui;

import com.platenplatter.util.PlannedMeal;
import com.platenplatter.util.TimeFormat;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/**
 * One line of a meal list: name, meal slot chip, healthy level chip (green / yellow / red),
 * prep tier chip (Quick / Average / Slow) with the minutes, the calories, and an optional Remove link.
 */
public final class MealRows {
    private MealRows() { }

    /** @param onRemove what to do when Remove is clicked; null means "no Remove link". */
    public static HBox row(PlannedMeal meal, Runnable onRemove) {
        return row(meal, onRemove, true);
    }

    /**
     * @param showSlot false when the list is already grouped by slot (the Today tab), so the slot chip
     *                 would only repeat the heading
     */
    public static HBox row(PlannedMeal meal, Runnable onRemove, boolean showSlot) {
        Label name = new Label(meal.getRecipeName());
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(260);
        name.setStyle("-fx-font-weight: bold;");
        name.setTooltip(new Tooltip(meal.getRecipeName() + " (" + meal.getCategory() + ")"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label health = Theme.healthChip(meal.getHealthyLevel());
        health.setMinWidth(Region.USE_PREF_SIZE);
        Label tier = Theme.tierChip(meal.getPrepTier());
        tier.setMinWidth(Region.USE_PREF_SIZE);

        Label minutes = new Label(TimeFormat.minutes(meal.getPrepMinutes()));
        minutes.getStyleClass().add("pnp-muted");
        minutes.setMinWidth(Region.USE_PREF_SIZE);

        Label calories = new Label(meal.getCalories() > 0 ? String.format("%,d kcal", meal.getCalories()) : "? kcal");
        calories.getStyleClass().add("pnp-muted");
        calories.setMinWidth(Region.USE_PREF_SIZE);
        if (meal.getCalories() <= 0) {
            calories.setTooltip(new Tooltip("Calories not given"));
        }

        HBox line = new HBox(8, name, spacer);
        if (showSlot) {
            Label slot = Theme.slotChip(meal.getMealSlot());
            slot.setMinWidth(Region.USE_PREF_SIZE);
            line.getChildren().add(slot);
        }
        line.getChildren().addAll(health, tier, minutes, calories);
        line.setAlignment(Pos.CENTER_LEFT);
        if (onRemove != null) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("pnp-small-button");
            remove.setOnAction(e -> onRemove.run());
            line.getChildren().add(remove);
        }
        return line;
    }
}
