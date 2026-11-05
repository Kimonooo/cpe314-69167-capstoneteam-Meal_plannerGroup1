package com.platenplatter.ui;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.CategoryMapper;
import com.platenplatter.util.DateTimeText;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.OverrunStyle;

import java.time.LocalDate;
import java.util.List;

/**
 * The "Recipes" tab.
 * What it does: search TheMealDB, read a recipe, and add it to the meal plan.
 * Rules we follow:
 *  - the website is NEVER called on the UI thread (a background Task does it);
 *  - saving a meal (with its ingredient list, for the shopping list) also runs in a background Task, so the window never freezes.
 * Controls that work: search box (Enter), Search button, quick-search buttons, results list,
 * date picker, prep-time spinner and the Add to plan button.
 */
public class RecipesScreen {
    private static final String[] QUICK_SEARCHES = {"chicken", "beef", "cake", "soup", "pasta"};
    private static final int MAX_PLANNED_SHOWN = 6;

    private final MealDbClient client;
    private final PlanStore store;
    private Runnable onPlanChanged = () -> { };

    private final BooleanProperty searching = new SimpleBooleanProperty(false);
    private final BooleanProperty adding = new SimpleBooleanProperty(false);

    private final VBox root = new VBox(14);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Search");
    private final ListView<MealData> resultsList = new ListView<>();
    private final Label resultsCount = new Label("Results");
    private final Label statusLabel = new Label();

    private final StackPane detailStack = new StackPane();
    private final VBox detailContent = new VBox(10);
    private final Label detailEmpty = new Label("Pick a recipe on the left to see its ingredients and steps.");
    private final Label detailName = new Label();
    private final FlowPane detailChips = new FlowPane(6, 6);
    private final FlowPane ingredientFlow = new FlowPane(6, 6);
    private final Label instructions = new Label();

    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final Spinner<Integer> prepSpinner = new Spinner<>();
    private final Button addButton = new Button("Add to my plan");
    private final VBox plannedBox = new VBox(6);
    private final VBox plannedList = new VBox(6);
    private final Label plannedHeader = new Label();

    public RecipesScreen(MealDbClient client, PlanStore store) {
        this.client = client;
        this.store = store;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildSearchRow(), buildMainArea(), statusLabel);
        statusLabel.setId("statusLabel");
        Theme.apply(root);
        Theme.status(statusLabel, "Type a recipe name and press Search.", Theme.Kind.INFO);
        wireEvents();
        showDetails(null);
        refreshPlanned();
    }

    public Parent getView() {
        return root;
    }

    /** Jumps the date picker to a day (used by the Meal Plan tab's "Add a meal" button). */
    public void setDate(LocalDate date) {
        datePicker.setValue(date);
    }

    /** Re-reads the "Planned for ..." list. */
    public void refresh() {
        refreshPlanned();
    }

    /** Called after a meal was saved, so other tabs (like Analytics) can refresh. */
    public void setOnPlanChanged(Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged == null ? () -> { } : onPlanChanged;
    }

    // ------------------------------------------------------------------ building the screen

    private VBox buildHeader() {
        Label title = new Label("Find a Recipe");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("Search TheMealDB, read the recipe, and add it to your meal plan.");
        subtitle.getStyleClass().add("pnp-subtitle");
        return new VBox(2, title, subtitle);
    }

    private VBox buildSearchRow() {
        searchField.setId("searchField");
        searchField.setPromptText("Try: chicken, adobo, cake...");
        searchField.getStyleClass().add("pnp-field");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        searchButton.setId("searchButton");
        searchButton.getStyleClass().add("pnp-button");
        searchButton.disableProperty().bind(searching);

        ProgressIndicator busy = new ProgressIndicator();
        busy.setPrefSize(22, 22);
        busy.setMaxSize(22, 22);
        busy.visibleProperty().bind(searching);
        busy.managedProperty().bind(searching);

        HBox row = new HBox(10, searchField, busy, searchButton);
        row.setAlignment(Pos.CENTER_LEFT);

        FlowPane quick = new FlowPane(8, 6);
        Label hint = new Label("Quick search:");
        hint.getStyleClass().add("pnp-muted");
        quick.getChildren().add(hint);
        for (String word : QUICK_SEARCHES) {
            Button b = new Button(word);
            b.getStyleClass().add("pnp-suggest");
            b.setOnAction(e -> {
                searchField.setText(word);
                doSearch();
            });
            quick.getChildren().add(b);
        }
        return new VBox(8, row, quick);
    }

    private HBox buildMainArea() {
        VBox left = buildResultsCard();
        left.setMinWidth(320);
        left.setPrefWidth(320);
        left.setMaxWidth(320);

        VBox right = new VBox(14, buildDetailCard(), buildPlanCard());
        HBox.setHgrow(right, Priority.ALWAYS);
        VBox.setVgrow(right.getChildren().get(0), Priority.ALWAYS);

        HBox main = new HBox(16, left, right);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private VBox buildResultsCard() {
        resultsCount.getStyleClass().add("pnp-section");
        resultsList.setId("resultsList");
        resultsList.getStyleClass().add("pnp-list");
        Label placeholder = new Label("Search for a recipe to get started.");
        placeholder.getStyleClass().add("pnp-empty");
        resultsList.setPlaceholder(placeholder);
        resultsList.setCellFactory(lv -> new MealCell());
        VBox.setVgrow(resultsList, Priority.ALWAYS);

        VBox card = new VBox(8, resultsCount, resultsList);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildDetailCard() {
        detailName.getStyleClass().add("pnp-title");
        detailName.setStyle("-fx-font-size: 21px;");
        detailName.setWrapText(true);

        Label ingTitle = new Label("Ingredients");
        ingTitle.getStyleClass().add("pnp-section");
        Label stepsTitle = new Label("How to cook it");
        stepsTitle.getStyleClass().add("pnp-section");

        instructions.setWrapText(true);
        instructions.setMaxWidth(Double.MAX_VALUE);
        ScrollPane steps = new ScrollPane(instructions);
        steps.setFitToWidth(true);
        steps.getStyleClass().add("pnp-scroll");
        steps.setMinHeight(90);
        VBox.setVgrow(steps, Priority.ALWAYS);

        detailContent.getChildren().addAll(detailName, detailChips, ingTitle, ingredientFlow, stepsTitle, steps);
        detailEmpty.getStyleClass().add("pnp-empty");
        detailEmpty.setWrapText(true);
        detailStack.getChildren().addAll(detailEmpty, detailContent);

        VBox card = new VBox(detailStack);
        VBox.setVgrow(detailStack, Priority.ALWAYS);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildPlanCard() {
        Label title = new Label("Add to my plan");
        title.getStyleClass().add("pnp-section");

        datePicker.setId("datePicker");
        datePicker.setEditable(false);
        prepSpinner.setId("prepSpinner");
        prepSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 480, 30, 5));
        prepSpinner.setPrefWidth(95);
        addButton.setId("addButton");
        addButton.getStyleClass().add("pnp-button");
        addButton.disableProperty().bind(resultsList.getSelectionModel().selectedItemProperty().isNull().or(adding));

        Label dateLabel = new Label("Date");
        dateLabel.getStyleClass().add("pnp-muted");
        Label prepLabel = new Label("Prep time (min)");
        prepLabel.getStyleClass().add("pnp-muted");
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox controls = new HBox(10, new VBox(3, dateLabel, datePicker), new VBox(3, prepLabel, prepSpinner), spacer, addButton);
        controls.setAlignment(Pos.BOTTOM_LEFT);

        plannedHeader.getStyleClass().add("pnp-muted");
        plannedHeader.setStyle("-fx-font-weight: bold;");
        plannedBox.getChildren().addAll(plannedHeader, plannedList);

        VBox card = new VBox(10, title, controls, plannedBox);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void wireEvents() {
        searchButton.setOnAction(e -> doSearch());
        searchField.setOnAction(e -> doSearch());
        addButton.setOnAction(e -> doAdd());
        resultsList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldMeal, newMeal) -> showDetails(newMeal));
        datePicker.valueProperty().addListener((obs, oldDate, newDate) -> refreshPlanned());
    }

    // ------------------------------------------------------------------ actions

    private void doSearch() {
        String query = searchField.getText() == null ? "" : searchField.getText().trim();
        if (query.isEmpty()) {
            Theme.status(statusLabel, "Please type something to search.", Theme.Kind.ERROR);
            return;
        }
        searching.set(true);
        Theme.status(statusLabel, "Searching for \"" + query + "\"...", Theme.Kind.INFO);

        // Background thread: the network call must NOT run on the UI thread.
        Task<List<MealData>> task = new Task<>() {
            @Override
            protected List<MealData> call() throws Exception {
                return client.searchByName(query);
            }
        };
        task.setOnSucceeded(e -> {
            List<MealData> meals = task.getValue();
            resultsList.getItems().setAll(meals);
            resultsCount.setText(meals.isEmpty() ? "Results" : "Results (" + meals.size() + ")");
            if (meals.isEmpty()) {
                Theme.status(statusLabel, "No recipes found for \"" + query + "\". Try another word.", Theme.Kind.ERROR);
            } else {
                resultsList.getSelectionModel().selectFirst();
                Theme.status(statusLabel, meals.size() + " recipe(s) found. Click one to read it.", Theme.Kind.OK);
            }
            searching.set(false);
        });
        task.setOnFailed(e -> {
            Theme.status(statusLabel, "Could not reach TheMealDB. Check your internet connection.", Theme.Kind.ERROR);
            searching.set(false);
        });
        runInBackground(task);
    }

    private void doAdd() {
        MealData meal = resultsList.getSelectionModel().getSelectedItem();
        LocalDate date = datePicker.getValue();
        Integer prep = prepSpinner.getValue();
        if (meal == null) {
            Theme.status(statusLabel, "Select a recipe first.", Theme.Kind.ERROR);
            return;
        }
        if (date == null || prep == null) {
            Theme.status(statusLabel, "Pick a date and a prep time first.", Theme.Kind.ERROR);
            return;
        }
        ImportRow row = new ImportRow(date.toString(), meal.getName(),
                CategoryMapper.toPlanCategory(meal.getCategory()), prep);
        adding.set(true);

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                store.addMeal(row, meal.getIngredients());
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            Theme.status(statusLabel, "Added \"" + meal.getName() + "\" to your plan on "
                    + DateTimeText.shortDate(date) + ".", Theme.Kind.OK);
            adding.set(false);
            refreshPlanned();
            onPlanChanged.run();
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            Theme.status(statusLabel, "Could not save the meal: " + (ex == null ? "unknown error" : ex.getMessage()),
                    Theme.Kind.ERROR);
            adding.set(false);
        });
        runInBackground(task);
    }

    private void refreshPlanned() {
        LocalDate date = datePicker.getValue();
        if (date == null) {
            plannedList.getChildren().clear();
            plannedHeader.setText("Pick a date to see its meals.");
            return;
        }
        plannedHeader.setText("Planned for " + DateTimeText.shortDate(date));
        Task<List<PlannedMeal>> task = new Task<>() {
            @Override
            protected List<PlannedMeal> call() throws Exception {
                return store.mealsOn(date);
            }
        };
        task.setOnSucceeded(e -> showPlanned(task.getValue()));
        task.setOnFailed(e -> {
            Label failed = new Label("Could not load the plan for this day.");
            failed.getStyleClass().add("pnp-muted");
            plannedList.getChildren().setAll(failed);
        });
        runInBackground(task);
    }

    // ------------------------------------------------------------------ showing things

    private void showDetails(MealData meal) {
        boolean has = meal != null;
        detailEmpty.setVisible(!has);
        detailEmpty.setManaged(!has);
        detailContent.setVisible(has);
        detailContent.setManaged(has);
        if (!has) {
            return;
        }
        detailName.setText(meal.getName());
        detailChips.getChildren().clear();
        String plan = CategoryMapper.toPlanCategory(meal.getCategory());
        if (!meal.getCategory().isBlank()) {
            detailChips.getChildren().add(Theme.chip(meal.getCategory(), Theme.chipClass(plan)));
        }
        if (!meal.getArea().isBlank()) {
            detailChips.getChildren().add(Theme.chip(meal.getArea(), "chip-neutral"));
        }
        detailChips.getChildren().add(Theme.chip(meal.getIngredients().size() + " ingredients", "chip-neutral"));

        ingredientFlow.getChildren().clear();
        for (String ingredient : meal.getIngredients()) {
            ingredientFlow.getChildren().add(Theme.chip(ingredient, "chip-neutral"));
        }
        instructions.setText(meal.getInstructions().isBlank() ? "No instructions were provided." : meal.getInstructions());
    }

    private void showPlanned(List<PlannedMeal> rows) {
        plannedList.getChildren().clear();
        if (rows.isEmpty()) {
            Label none = new Label("Nothing planned for this day yet.");
            none.getStyleClass().add("pnp-empty");
            plannedList.getChildren().add(none);
            return;
        }
        int shown = Math.min(rows.size(), MAX_PLANNED_SHOWN);
        for (int i = 0; i < shown; i++) {
            plannedList.getChildren().add(MealRows.row(rows.get(i), null));
        }
        if (rows.size() > shown) {
            Label more = new Label("+ " + (rows.size() - shown) + " more");
            more.getStyleClass().add("pnp-muted");
            plannedList.getChildren().add(more);
        }
    }

    private static void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    /** One row in the results list: the recipe name, its cuisine, and a colored category chip. */
    private static class MealCell extends ListCell<MealData> {
        private final Label name = new Label();
        private final Label sub = new Label();
        private final Label chip = new Label();
        private final HBox box = new HBox(8);

        MealCell() {
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13.5px;");
            name.setTextOverrun(OverrunStyle.ELLIPSIS);
            sub.getStyleClass().add("pnp-muted");
            sub.setStyle("-fx-font-size: 12px;");
            VBox texts = new VBox(1, name, sub);
            HBox.setHgrow(texts, Priority.ALWAYS);
            texts.setMaxWidth(Double.MAX_VALUE);
            chip.getStyleClass().add("pnp-chip");
            chip.setMinWidth(Region.USE_PREF_SIZE); // never squeeze the chip into "..."
            box.setAlignment(Pos.CENTER_LEFT);
            box.getChildren().addAll(texts, chip);
            setPrefWidth(0);
        }

        @Override
        protected void updateItem(MealData meal, boolean empty) {
            super.updateItem(meal, empty);
            if (empty || meal == null) {
                setGraphic(null);
                return;
            }
            name.setText(meal.getName());
            sub.setText(meal.getArea().isBlank() ? "Recipe" : meal.getArea() + " cuisine");
            chip.setText(meal.getCategory());
            chip.getStyleClass().removeAll("chip-breakfast", "chip-main", "chip-dessert", "chip-neutral");
            chip.getStyleClass().add(Theme.chipClass(CategoryMapper.toPlanCategory(meal.getCategory())));
            chip.setVisible(!meal.getCategory().isBlank());
            setGraphic(box);
        }
    }
}
