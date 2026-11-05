package com.platenplatter.ui;

import com.platenplatter.util.PlannedMeal;
import com.platenplatter.util.TimeFormat;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** One line of a meal list: name, colored category chip, prep time and an optional Remove link. */
public final class MealRows {
    private MealRows() { }

    /** @param onRemove what to do when Remove is clicked; null means "no Remove link". */
    public static HBox row(PlannedMeal meal, Runnable onRemove) {
        Label name = new Label(meal.getRecipeName());
        name.setTextOverrun(OverrunStyle.ELLIPSIS);
        name.setMaxWidth(300);
        name.setStyle("-fx-font-weight: bold;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label minutes = new Label(TimeFormat.minutes(meal.getPrepMinutes()));
        minutes.getStyleClass().add("pnp-muted");
        minutes.setMinWidth(Region.USE_PREF_SIZE);

        Label chip = Theme.chip(meal.getCategory(), Theme.chipClass(meal.getCategory()));
        chip.setMinWidth(Region.USE_PREF_SIZE);

        HBox line = new HBox(10, name, spacer, chip, minutes);
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
