package com.platenplatter.ui;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.CategoryMapper;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.RowSink;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.Spinner;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/**
 * "Recipes" tab. Responsibility: search TheMealDB and let the user add a recipe to the plan.
 * - Search runs in a background Task (the API is NEVER called on the UI thread).
 * - "Add to plan" hands an ImportRow to the RowSink (the database saves it; Kimberly wires it in).
 * Controls (all wired): search field (Enter), Search button, results list, date picker,
 * prep-time spinner, Add to plan button.
 */
public class RecipesScreen {
    private final MealDbClient client;
    private final RowSink sink;

    private final VBox root = new VBox(10);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Search");
    private final ListView<MealData> results = new ListView<>();
    private final TextArea details = new TextArea();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final Spinner<Integer> prepSpinner = new Spinner<>(1, 480, 30);
    private final Button addButton = new Button("Add to plan");
    private final Label status = new Label("Type a recipe name and press Search.");

    public RecipesScreen(MealDbClient client, RowSink sink) {
        this.client = client;
        this.sink = sink;

        Label title = new Label("Find a Recipe");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5C0008;");

        searchField.setPromptText("e.g. chicken, adobo, cake");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        styleButton(searchButton);
        styleButton(addButton);
        HBox searchRow = new HBox(10, searchField, searchButton);

        results.setPrefHeight(180);
        details.setEditable(false);
        details.setWrapText(true);
        details.setPrefRowCount(6);
        details.setPromptText("Select a recipe to see its ingredients and instructions.");

        HBox planRow = new HBox(10, new Label("Date:"), datePicker,
                new Label("Prep (min):"), prepSpinner, addButton);
        planRow.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(title, searchRow, results, details, planRow, status);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFBD0;");

        searchButton.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        addButton.setOnAction(e -> doAdd());
        results.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldMeal, newMeal) -> showDetails(newMeal));
    }

    public Parent getView() {
        return root;
    }

    private void doSearch() {
        String query = searchField.getText();
        if (query == null || query.isBlank()) {
            status.setText("Please type something to search.");
            return;
        }
        searchButton.setDisable(true);
        status.setText("Searching...");

        // Background thread: the network call must NOT run on the UI thread.
        Task<List<MealData>> task = new Task<>() {
            @Override
            protected List<MealData> call() throws Exception {
                return client.searchByName(query);
            }
        };
        task.setOnSucceeded(e -> {
            List<MealData> meals = task.getValue();
            results.getItems().setAll(meals);
            status.setText(meals.isEmpty()
                    ? "No recipes found for \"" + query + "\"."
                    : meals.size() + " recipe(s) found.");
            searchButton.setDisable(false);
        });
        task.setOnFailed(e -> {
            status.setText("Could not reach TheMealDB. Check your internet connection.");
            searchButton.setDisable(false);
        });
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void doAdd() {
        MealData meal = results.getSelectionModel().getSelectedItem();
        if (meal == null) {
            status.setText("Select a recipe first.");
            return;
        }
        LocalDate date = datePicker.getValue();
        if (date == null) {
            status.setText("Pick a date for the meal.");
            return;
        }
        try {
            sink.accept(new ImportRow(date.toString(), meal.getName(),
                    CategoryMapper.toPlanCategory(meal.getCategory()), prepSpinner.getValue()));
            status.setText("Added \"" + meal.getName() + "\" to your plan on " + date + ".");
        } catch (Exception ex) {
            status.setText("Could not save the meal: " + ex.getMessage());
        }
    }

    private void showDetails(MealData meal) {
        if (meal == null) {
            details.clear();
            return;
        }
        details.setText("Category: " + meal.getCategory() + "   |   Cuisine: " + meal.getArea()
                + "\n\nIngredients: " + String.join(", ", meal.getIngredients())
                + "\n\n" + meal.getInstructions());
    }

    private static void styleButton(Button b) {
        b.setStyle("-fx-background-color: #5C0008; -fx-text-fill: white;");
    }
}
