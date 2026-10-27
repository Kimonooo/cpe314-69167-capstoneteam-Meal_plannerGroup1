package com.platenplatter.ui;

import java.time.LocalDate;
import java.time.YearMonth;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** A monthly calendar grid. Day 1: layout + clicking a day. Day 2: show planned meals. */
public class CalendarScreen {
    private final YearMonth month;
    private final Label selectedLabel = new Label("Click a day");

    public CalendarScreen() {
        this(YearMonth.now());
    }

    public CalendarScreen(YearMonth month) {
        this.month = month;
    }

    public Parent getView() {
        Label title = new Label(month.getMonth() + " " + month.getYear());
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5A000A;");

        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        grid.setAlignment(Pos.CENTER);

        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int column = 0; column < 7; column++) {
            Label header = new Label(dayNames[column]);
            header.setStyle("-fx-font-weight: bold;");
            grid.add(header, column, 0);
        }

        // Monday = 1 ... Sunday = 7, so subtract 1 to get the starting column (0 to 6)
        int column = month.atDay(1).getDayOfWeek().getValue() - 1;
        int row = 1;
        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            LocalDate date = month.atDay(day);
            Button dayButton = new Button(String.valueOf(day));
            dayButton.setPrefSize(55, 40);
            // Event handler: runs when this day is clicked
            dayButton.setOnAction(event -> selectedLabel.setText("Selected: " + date));
            grid.add(dayButton, column, row);
            column++;
            if (column == 7) {
                column = 0;
                row++;
            }
        }

        VBox root = new VBox(15, title, grid, selectedLabel);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FFFBD0;");
        return root;
    }
}
