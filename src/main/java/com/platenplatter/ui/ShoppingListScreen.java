package com.platenplatter.ui;

import com.platenplatter.controller.ShoppingListController;
import com.platenplatter.controller.ShoppingListController.ListData;
import com.platenplatter.controller.ShoppingListController.ShownList;
import com.platenplatter.controller.UserMessages;
import com.platenplatter.service.ShoppingList;
import com.platenplatter.service.StatisticsService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
 * list (those buttons are off while the list is empty). Recipes planned without ingredients (from
 * a CSV import) can be looked up online on TheMealDB. Every handler calls one method of
 * ShoppingListController; the page itself holds no rules.
 */
public class ShoppingListScreen {
    private final ShoppingListController controller;
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final ComboBox<ShoppingList.RecipeChoice> recipeBox = new ComboBox<>();
    private boolean fillingChoices;
    private ListData data = ListData.EMPTY;
    private ShownList shown = new ShownList(Map.of(), List.of(), "");
    private int request;                                 // only the newest read may draw the list
    private final Label summary = new Label();
    private final VBox groupsBox = new VBox(6);
    private final HBox missingBox = new HBox(10);
    private final Label missingText = new Label();
    private final Button lookupButton = new Button("Look up online");
    private final Button copyButton = new Button("Copy list");
    private final Button saveButton = new Button("Save as text file...");
    private final Button untickButton = new Button("Untick all");
    private final Label status = new Label();
    private final Set<String> ticked = new HashSet<>();

    public ShoppingListScreen(ShoppingListController controller) {
        this(controller, LocalDate::now);
    }

    public ShoppingListScreen(ShoppingListController controller,
            Supplier<LocalDate> todayProvider) {
        this.controller = controller;
        HBox range = buildRange(todayProvider);
        HBox recipeRow = buildRecipeRow();
        setUpListControls();

        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(
                card(Theme.sectionTitle("Which days and which recipe?", "food"), range, recipeRow),
                card(Theme.sectionTitle("Your list", "dishes"), summary,
                        new HBox(10, copyButton, saveButton, untickButton), missingBox, groupsBox),
                status);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        refresh();
    }

    /** The From and To pickers (this week at first) and the This week button. */
    private HBox buildRange(Supplier<LocalDate> todayProvider) {
        LocalDate monday = StatisticsService.mondayOf(todayProvider.get());
        Stream.of(fromPicker, toPicker).forEach(Theme::dateFormat);   // "Mon, 9 Nov 2026"
        fromPicker.setValue(monday);
        toPicker.setValue(monday.plusDays(6));
        fromPicker.setId("shopFrom");
        toPicker.setId("shopTo");
        fromPicker.valueProperty().addListener((obs, old, now) -> refresh());
        toPicker.valueProperty().addListener((obs, old, now) -> refresh());
        Button thisWeek = button(new Button("This week"), "pnp-nav-button", () -> {
            LocalDate start = StatisticsService.mondayOf(todayProvider.get());
            fromPicker.setValue(start);
            toPicker.setValue(start.plusDays(6));
        });
        HBox range = new HBox(10, Theme.labeled("From", fromPicker), Theme.labeled("To", toPicker),
                thisWeek);
        range.setAlignment(Pos.BOTTOM_LEFT);
        return range;
    }

    /** The recipe choice: one recipe or "All planned recipes". */
    private HBox buildRecipeRow() {
        recipeBox.setId("shopRecipe");
        recipeBox.setPromptText("No planned recipes on these days");
        recipeBox.setPrefWidth(320);
        recipeBox.valueProperty().addListener((obs, old, now) -> {
            if (!fillingChoices) {
                ticked.clear(); // another recipe was picked: it starts unticked
                showChoice();
            }
        });
        Label recipeHint = Theme.text(
                "Choose one recipe, or \"All planned recipes\" for everything.", "pnp-hint");
        HBox recipeRow = new HBox(10, Theme.labeled("Recipe", recipeBox), recipeHint);
        recipeRow.setAlignment(Pos.BOTTOM_LEFT);
        return recipeRow;
    }

    /** Copy, Save and Untick (off while the list is empty), the summary and the missing note. */
    private void setUpListControls() {
        copyButton.setId("copyListButton");
        saveButton.setId("saveListButton");
        untickButton.setId("untickButton");
        button(copyButton, "pnp-button-secondary", this::copyList);
        button(saveButton, "pnp-button-secondary", this::saveList);
        button(untickButton, "pnp-button-secondary", () -> {
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
        int mine = ++request;
        Task<ListData> task = new Task<>() {
            @Override
            protected ListData call() throws Exception {
                return controller.load(from, to);
            }
        };
        task.setOnSucceeded(e -> {
            if (mine != request) {
                return; // the days changed again: a newer read is coming
            }
            data = task.getValue();
            fillChoices(controller.choices(data));
            showChoice();
            Theme.status(status, "", Theme.Kind.INFO);
        });
        task.setOnFailed(e -> Theme.status(status, UserMessages.of(task.getException()),
                Theme.Kind.ERROR));
        AppShell.runInBackground(task);
    }

    /**
     * Puts the planned recipes in the box, with "All planned recipes" last. The recipe that was
     * chosen stays chosen when it is still planned; otherwise the first recipe is chosen.
     */
    private void fillChoices(List<ShoppingList.RecipeChoice> recipes) {
        ShoppingList.RecipeChoice before = recipeBox.getValue();
        fillingChoices = true; // setting the items must not redraw the list yet
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
        shown = controller.build(data, recipeBox.getValue());
        summary.setText(shown.summary());
        drawList();
    }

    private void drawList() {
        missingText.setText("No ingredient list yet for: " + String.join(", ", shown.missing()));
        Theme.show(missingBox, !shown.missing().isEmpty());
        copyButton.setDisable(shown.isEmpty());          // nothing to copy, save or untick yet
        saveButton.setDisable(shown.isEmpty());
        untickButton.setDisable(shown.isEmpty() || ticked.isEmpty());
        String emptyText = data.meals().isEmpty()
                ? "Nothing to buy yet. Plan a meal on the Recipes page."
                : "No ingredients are saved for this choice yet.";
        groupsBox.getChildren().setAll(shown.isEmpty()
                ? List.<Node>of(Theme.emptyState(emptyText, "meal"))
                : shown.groups().entrySet().stream().flatMap(group -> Stream.concat(
                        Stream.of(Theme.text(group.getKey().getLabel()
                                + " (" + group.getValue().size() + ")", "pnp-group-header")),
                        group.getValue().entrySet().stream()
                                .map(item -> checkBox(item.getKey(), item.getValue()))))
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
            untickButton.setDisable(ticked.isEmpty());
        });
        if (box.isSelected()) {
            box.getStyleClass().add("pnp-check-done");
        }
        return box;
    }

    private void copyList() {
        ClipboardContent content = new ClipboardContent();
        content.putString(controller.asText(recipeBox.getValue(), shown));
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
            controller.save(file.toPath(), controller.asText(recipeBox.getValue(), shown));
            Theme.status(status, "Saved to " + file.getName() + ".", Theme.Kind.OK);
        } catch (IOException e) {
            Theme.status(status,
                    "Could not save the file there. Pick another folder and try again.",
                    Theme.Kind.ERROR);
        }
    }

    /**
     * Looks up each missing recipe on TheMealDB in the background; only an exact match is saved.
     */
    private void lookUpMissing() {
        List<String> names = shown.missing();
        lookupButton.setDisable(true);
        Theme.status(status, "Looking up " + names.size() + " recipes...", Theme.Kind.INFO);
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return controller.lookUpMissing(names);
            }
        };
        task.setOnSucceeded(e -> {
            lookupButton.setDisable(false);
            Theme.status(status, "Found ingredients for " + task.getValue() + " of " + names.size()
                    + " recipes.", Theme.Kind.OK);
            refresh();
        });
        task.setOnFailed(e -> {
            lookupButton.setDisable(false);
            Theme.status(status, UserMessages.of(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    private static Button button(Button button, String style, Runnable action) {
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
