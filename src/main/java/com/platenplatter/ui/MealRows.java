package com.platenplatter.ui;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.util.TextFormat;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** One planned meal as a row, the same on Today and on the Meal Plan page. */
public final class MealRows {
    private MealRows() { }

    /** The recipe name, then its chips: slot (optional), healthy level, prep tier, minutes and calories. */
    public static HBox row(PlannedMeal meal, Runnable onRemove, boolean showSlot) {
        Label name = new Label(meal.getRecipeName());
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(280);
        name.setStyle("-fx-font-weight: bold;");
        name.setTooltip(new Tooltip(meal.getRecipeName() + " (" + meal.getRecipe().getMealTypeLabel() + ")"));
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox line = new HBox(8, name, gap);
        if (showSlot) {
            line.getChildren().add(Theme.slotChip(meal.getMealSlot()));
        }
        line.getChildren().addAll(Theme.healthChip(meal.getHealthyLevel()), Theme.tierChip(meal.getPrepTier()),
                Theme.text(TextFormat.minutes(meal.getPrepMinutes()), "pnp-muted"),
                Theme.text(TextFormat.calories(meal.getCalories()), "pnp-muted"));
        if (onRemove != null) {
            Button remove = new Button("Remove");
            remove.getStyleClass().add("pnp-small-button");
            remove.setOnAction(e -> onRemove.run());
            line.getChildren().add(remove);
        }
        line.getChildren().stream().skip(2).forEach(node -> ((Region) node).setMinWidth(Region.USE_PREF_SIZE));
        line.setAlignment(Pos.CENTER_LEFT);
        return line;
    }
}
