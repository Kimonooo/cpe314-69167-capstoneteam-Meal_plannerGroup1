package com.platenplatter.ui;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.ShoppingList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * The Shopping List page. The user picks the days and then chooses ONE planned recipe: the list
 * shows only that recipe's ingredients, grouped by food group. "All planned recipes" is the other
 * choice and puts everything on one list. Tick what is already in the basket; copy or save the
 * list. Recipes planned without ingredients (from a CSV import) can be looked up online on TheMealDB.
 */
public class ShoppingListScreen {
    private record ListData(List<PlannedMeal> meals, List<IngredientAmount> items) { }

    private final PlanStore store;
    private final MealDbClient client;
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final ComboBox<ShoppingList.RecipeChoice> recipeBox = new ComboBox<>();
    private boolean fillingChoices;
    private ListData data = new ListData(List.of(), List.of());
    private final Label summary = new Label();
    private final VBox groupsBox = new VBox(6);
    private final HBox missingBox = new HBox(10);
    private final Label missingText = new Label();
    private final Button lookupButton = new Button("Look up online");
    private final Label status = new Label();
    private final Set<String> ticked = new HashSet<>();
    private Map<FoodGroup, Map<String, List<String>>> list = Map.of();
    private List<String> missing = List.of();

    public ShoppingListScreen(PlanStore store, MealDbClient client) {
        this(store, client, LocalDate::now);
    }

    public ShoppingListScreen(PlanStore store, MealDbClient client, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.client = client;
        LocalDate monday = StatisticsService.mondayOf(todayProvider.get());
        fromPicker.setValue(monday);
        toPicker.setValue(monday.plusDays(6));
        fromPicker.setId("shopFrom");
        toPicker.setId("shopTo");
        fromPicker.valueProperty().addListener((obs, old, now) -> refresh());
        toPicker.valueProperty().addListener((obs, old, now) -> refresh());
        Button thisWeek = button("This week", "pnp-nav-button", () -> {
            LocalDate start = StatisticsService.mondayOf(todayProvider.get());
            fromPicker.setValue(start);
            toPicker.setValue(start.plusDays(6));
        });
        HBox range = new HBox(10, Theme.text("From", "pnp-muted"), fromPicker, Theme.text("To", "pnp-muted"), toPicker, thisWeek);
        range.setAlignment(Pos.CENTER_LEFT);

        recipeBox.setId("shopRecipe");
        recipeBox.setPromptText("No planned recipes on these days");
        recipeBox.setPrefWidth(320);
        recipeBox.valueProperty().addListener((obs, old, now) -> {
            if (!fillingChoices) {
                ticked.clear();                            // the user picked another recipe: it starts unticked
                showChoice();
            }
        });
        HBox recipeRow = new HBox(10, Theme.text("Recipe", "pnp-muted"), recipeBox,
                Theme.text("Choose one recipe, or \"All planned recipes\" for everything.", "pnp-muted"));
        recipeRow.setAlignment(Pos.CENTER_LEFT);

        Button copy = button("Copy list", "pnp-button-secondary", this::copyList);
        Button save = button("Save as text file...", "pnp-button-secondary", this::saveList);
        Button untick = button("Untick all", "pnp-button-secondary", () -> {
            ticked.clear();
            drawList();
        });
        summary.setId("shopSummary");
        summary.getStyleClass().add("pnp-muted");
        missingText.setWrapText(true);
        missingText.getStyleClass().add("pnp-badge");
        HBox.setHgrow(missingText, Priority.ALWAYS);
        lookupButton.setId("lookupButton");
        lookupButton.getStyleClass().add("pnp-button");
        lookupButton.setOnAction(e -> lookUpMissing());
        missingBox.getChildren().addAll(missingText, lookupButton);
        missingBox.getStyleClass().add("pnp-note");
        missingBox.setAlignment(Pos.CENTER_LEFT);
        Theme.show(missingBox, false);
        status.setId("shopStatus");

        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(
                card(Theme.sectionTitle("Which days and which recipe?", "food"), range, recipeRow),
                card(Theme.sectionTitle("Your list", "dishes"), summary, new HBox(10, copy, save, untick), missingBox, groupsBox),
                status);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        refresh();
    }

    public Parent getView() {
        return scroller;
    }

    /** Reads the meals and their ingredients for the chosen days, in the background. */
    public void refresh() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null || to.isBefore(from)) {
            Theme.status(status, "Pick a From day that is not after the To day.", Theme.Kind.ERROR);
            return;
        }
        Task<ListData> task = new Task<>() {
            @Override
            protected ListData call() throws Exception {
                return new ListData(store.mealsBetween(from, to), store.shoppingItems(from, to));
            }
        };
        task.setOnSucceeded(e -> {
            data = task.getValue();
            fillChoices(ShoppingList.recipeChoices(data.meals()));
            showChoice();
            Theme.status(status, "", Theme.Kind.INFO);
        });
        task.setOnFailed(e -> Theme.status(status, "Could not read the meal plan.", Theme.Kind.ERROR));
        AppShell.runInBackground(task);
    }

    /**
     * Puts the planned recipes in the box, with "All planned recipes" last. The recipe that was
     * chosen stays chosen when it is still planned; otherwise the first recipe is chosen.
     */
    private void fillChoices(List<ShoppingList.RecipeChoice> recipes) {
        ShoppingList.RecipeChoice before = recipeBox.getValue();
        fillingChoices = true;                             // setting the items must not redraw the list yet
        recipeBox.getItems().setAll(recipes);
        if (recipes.isEmpty()) {
            recipeBox.setValue(null);
        } else {
            recipeBox.getItems().add(ShoppingList.ALL_RECIPES);
            recipeBox.setValue(recipeBox.getItems().contains(before) ? before : recipes.get(0));
        }
        recipeBox.setDisable(recipes.isEmpty());
        fillingChoices = false;
    }

    /** Builds the list for the chosen recipe (or for all of them) and draws it. */
    private void showChoice() {
        ShoppingList.RecipeChoice choice = recipeBox.getValue();
        if (choice == null || ShoppingList.ALL_RECIPES.equals(choice)) {
            list = ShoppingList.build(ShoppingList.distinct(data.items()));
            missing = ShoppingList.missingRecipes(data.meals(), data.items());
            summary.setText(ShoppingList.count(list) + " ingredients for all " + data.meals().size() + " planned meals.");
        } else {
            List<IngredientAmount> items = ShoppingList.forRecipe(data.items(), choice.savedRecipeId());
            List<PlannedMeal> meals = ShoppingList.mealsOf(data.meals(), choice.savedRecipeId());
            list = ShoppingList.build(items);
            missing = ShoppingList.missingRecipes(meals, items);
            summary.setText(ShoppingList.count(list) + " ingredients for " + choice.name()
                    + " (planned " + meals.size() + (meals.size() == 1 ? " time)." : " times)."));
        }
        drawList();
    }

    /** The first line of a copied or saved list: which recipe it is for. */
    private String listTitle() {
        ShoppingList.RecipeChoice choice = recipeBox.getValue();
        return "Shopping list: " + (choice == null ? ShoppingList.ALL_RECIPES.name() : choice.name());
    }

    private void drawList() {
        missingText.setText("No ingredient list yet for: " + String.join(", ", missing));
        Theme.show(missingBox, !missing.isEmpty());
        groupsBox.getChildren().setAll(list.isEmpty()
                ? List.<Node>of(Theme.emptyState(data.meals().isEmpty() ? "Nothing to buy yet. Plan a meal on the Recipes page."
                        : "No ingredients are saved for this choice yet.", "meal"))
                : list.entrySet().stream().flatMap(group -> Stream.concat(
                        Stream.of(Theme.text(group.getKey().getLabel()
                                + " (" + group.getValue().size() + ")", "pnp-group-header")),
                        group.getValue().entrySet().stream().map(item -> checkBox(item.getKey(), item.getValue()))))
                        .toList());
    }

    private Node checkBox(String ingredient, List<String> measures) {
        CheckBox box = new CheckBox(ShoppingList.line(ingredient, measures));
        box.getStyleClass().add("pnp-check");
        box.setSelected(ticked.contains(ingredient));
        box.selectedProperty().addListener((obs, old, isTicked) -> {
            if (isTicked) {
                ticked.add(ingredient);
                box.getStyleClass().add("pnp-check-done");
            } else {
                ticked.remove(ingredient);
                box.getStyleClass().remove("pnp-check-done");
            }
        });
        if (box.isSelected()) {
            box.getStyleClass().add("pnp-check-done");
        }
        return box;
    }

    private void copyList() {
        ClipboardContent content = new ClipboardContent();
        content.putString(listTitle() + "\n\n" + ShoppingList.asText(list));
        Clipboard.getSystemClipboard().setContent(content);
        Theme.status(status, "Copied! Paste it in a chat or a note.", Theme.Kind.OK);
    }

    private void saveList() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save shopping list");
        chooser.setInitialFileName("shopping_list.txt");
        File file = chooser.showSaveDialog(root.getScene().getWindow());
        if (file == null) {
            return;
        }
        try {
            Files.writeString(file.toPath(), listTitle() + "\n\n" + ShoppingList.asText(list));
            Theme.status(status, "Saved to " + file.getName() + ".", Theme.Kind.OK);
        } catch (IOException e) {
            Theme.status(status, "Could not save the file: " + e.getMessage(), Theme.Kind.ERROR);
        }
    }

    /** Looks up each missing recipe on TheMealDB; only an exact name match is saved. */
    private void lookUpMissing() {
        List<String> names = missing;
        lookupButton.setDisable(true);
        Theme.status(status, "Looking up " + names.size() + " recipes...", Theme.Kind.INFO);
        Task<Long> task = new Task<>() {
            @Override
            protected Long call() {
                return names.stream().filter(name -> lookUp(name)).count();
            }
        };
        task.setOnSucceeded(e -> {
            lookupButton.setDisable(false);
            Theme.status(status, "Found ingredients for " + task.getValue() + " of " + names.size() + " recipes.", Theme.Kind.OK);
            refresh();
        });
        task.setOnFailed(e -> {
            lookupButton.setDisable(false);
            Theme.status(status, MealDbClient.isOffline(task.getException())
                    ? "No internet connection. Check your Wi-Fi, then try again."
                    : "TheMealDB is not responding right now.", Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    /** True when an exact match was found and its ingredients were saved. */
    private boolean lookUp(String recipeName) {
        try {
            Optional<MealData> found = client.findExact(recipeName);
            if (found.isPresent()) {
                store.saveIngredients(recipeName, found.get().ingredients());
            }
            return found.isPresent();
        } catch (Exception e) {
            throw new IllegalStateException("Look-up failed for " + recipeName, e);
        }
    }

    private static Button button(String text, String style, Runnable action) {
        Button button = new Button(text);
        button.getStyleClass().add(style);
        button.setOnAction(e -> action.run());
        return button;
    }

    private static VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.getStyleClass().add("pnp-card");
        return card;
    }
}
