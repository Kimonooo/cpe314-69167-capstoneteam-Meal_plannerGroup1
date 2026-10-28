package com.platenplatter.ui;

import com.platenplatter.model.BreakfastRecipe;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.MainCourseRecipe;
import com.platenplatter.model.MealPlanEntry;
import com.platenplatter.model.Recipe;
import com.platenplatter.util.MealPlanBook;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/**
 * A monthly calendar. Days that have planned meals are gold.
 * Click a day to see its meals underneath. (Day 2: uses a hard-coded sample list.)
 */
public class CalendarScreen {
    private final YearMonth month;
    private final MealPlanBook book;
    private final Label selectedLabel = new Label("Click a day to see its meals");
    private final VBox mealsBox = new VBox(4);

    public CalendarScreen() {
        this(YearMonth.now(), sampleBook(YearMonth.now()));
    }

    public CalendarScreen(YearMonth month, MealPlanBook book) {
        this.month = month;
        this.book = book;
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
            if (book.hasMeals(date)) {
                dayButton.setStyle("-fx-background-color: #FFD84D;"); // a day with meals
            }
            // Event handler: runs when this day is clicked
            dayButton.setOnAction(event -> showDay(date));
            grid.add(dayButton, column, row);
            column++;
            if (column == 7) {
                column = 0;
                row++;
            }
        }

        VBox root = new VBox(15, title, grid, selectedLabel, mealsBox);
        root.setPadding(new Insets(20));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #FFFBD0;");
        return root;
    }

    /** Shows the meals planned on one day. */
    private void showDay(LocalDate date) {
        selectedLabel.setText("Meals on " + date);
        mealsBox.getChildren().clear();
        List<MealPlanEntry> meals = book.getMealsOn(date);
        if (meals.isEmpty()) {
            mealsBox.getChildren().add(new Label("No meals planned."));
            return;
        }
        for (MealPlanEntry entry : meals) {
            Recipe recipe = entry.getRecipe();
            mealsBox.getChildren().add(new Label(entry.getMealSlot() + ": " + recipe.getName()
                    + " (" + recipe.getPrepMinutes() + " min)"));
        }
    }

    /** Hard-coded sample meals, so the calendar has something to show before the database does. */
    private static MealPlanBook sampleBook(YearMonth month) {
        MealPlanBook sample = new MealPlanBook();
        Recipe oats = new BreakfastRecipe(1, "Overnight Oats", "American", 10, 300);
        Recipe pasta = new MainCourseRecipe(2, "Pasta Pomodoro", "Italian", 20, 550);
        Recipe cake = new DessertRecipe(3, "Chocolate Cake", "American", 30, 400);
        sample.add(new MealPlanEntry(1, month.atDay(3), "Breakfast", oats));
        sample.add(new MealPlanEntry(2, month.atDay(3), "Dinner", pasta));
        sample.add(new MealPlanEntry(3, month.atDay(10), "Dessert", cake));
        sample.add(new MealPlanEntry(4, month.atDay(15), "Breakfast", oats));
        return sample;
    }
}
