package com.platenplatter.ui;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.util.DateTimeText;
import com.platenplatter.util.IngredientLookup;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.ShoppingList;
import com.platenplatter.util.ShoppingPlanner;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * The "Shopping List" tab.
 * Pick a range of days (Today, Next 7 days, This week, This month, or your own dates) and the app builds
 * ONE list from every meal planned in that range: the same ingredient from several meals is a single
 * line ("for 3 meals"), and the lines are grouped by shop aisle, each aisle sorted A to Z.
 * Tick items as you buy them (ticks are remembered), copy the list, or save it as a text file.
 * Recipes with no ingredient list are shown in a yellow box with a "Look up online" button.
 * Everything slow (database, internet, saving a file) runs in a background Task.
 */
public class ShoppingListScreen {
    private final PlanStore store;
    private final MealDbClient client;
    private final Supplier<LocalDate> todayProvider;

    private final BooleanProperty busy = new SimpleBooleanProperty(false);
    private final Set<String> checked = new HashSet<>();
    private ShoppingList current;
    private boolean updatingDates;

    private final VBox root = new VBox(14);
    private final ToggleGroup presets = new ToggleGroup();
    private final ToggleButton todayToggle = new ToggleButton("Today");
    private final ToggleButton weekAheadToggle = new ToggleButton("Next 7 days");
    private final ToggleButton thisWeekToggle = new ToggleButton("This week");
    private final ToggleButton monthToggle = new ToggleButton("This month");
    private final DatePicker fromPicker = new DatePicker();
    private final DatePicker toPicker = new DatePicker();
    private final Button copyButton = new Button("Copy list");
    private final Button saveButton = new Button("Save as text file...");
    private final Button uncheckButton = new Button("Untick all");
    private final Label summaryLabel = new Label();
    private final ProgressBar boughtBar = new ProgressBar(0);
    private final Label boughtLabel = new Label();
    private final VBox listBox = new VBox(14);
    private final VBox missingBox = new VBox(8);
    private final Label statusLabel = new Label();

    public ShoppingListScreen(PlanStore store, MealDbClient client) {
        this(store, client, LocalDate::now);
    }

    public ShoppingListScreen(PlanStore store, MealDbClient client, Supplier<LocalDate> todayProvider) {
        this.store = store;
        this.client = client;
        this.todayProvider = todayProvider;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildMain(), statusLabel);
        Theme.apply(root);
        statusLabel.setId("shopStatus");
        Theme.status(statusLabel, "Pick the days you are shopping for.", Theme.Kind.INFO);
        selectPreset(weekAheadToggle);
    }

    public Parent getView() {
        return root;
    }

    /** Rebuilds the list from the database (keeps your ticks). */
    public void refresh() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        if (from == null || to == null) {
            return;
        }
        if (to.isBefore(from)) {
            Theme.status(statusLabel, "The end day is before the start day. Please fix the dates.", Theme.Kind.ERROR);
            return;
        }
        busy.set(true);
        Task<ShoppingList> task = new Task<>() {
            @Override
            protected ShoppingList call() throws Exception {
                return ShoppingPlanner.plan(store, from, to);
            }
        };
        task.setOnSucceeded(e -> {
            show(task.getValue(), from, to);
            busy.set(false);
        });
        task.setOnFailed(e -> {
            Theme.status(statusLabel, "Could not build the list: " + message(task.getException()), Theme.Kind.ERROR);
            busy.set(false);
        });
        runInBackground(task);
    }

    // ------------------------------------------------------------------ building the screen

    private VBox buildHeader() {
        Label title = new Label("Shopping List");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("Everything you need to buy for the meals you planned, sorted by shop aisle.");
        subtitle.getStyleClass().add("pnp-subtitle");
        return new VBox(2, title, subtitle);
    }

    private HBox buildMain() {
        VBox controls = buildControlsCard();
        controls.setMinWidth(260);
        controls.setPrefWidth(270);
        controls.setMaxWidth(270);
        VBox listCard = buildListCard();
        HBox.setHgrow(listCard, Priority.ALWAYS);
        HBox main = new HBox(16, controls, listCard);
        VBox.setVgrow(main, Priority.ALWAYS);
        return main;
    }

    private VBox buildControlsCard() {
        Label rangeTitle = new Label("Which days?");
        rangeTitle.getStyleClass().add("pnp-section");

        for (ToggleButton t : new ToggleButton[] {todayToggle, weekAheadToggle, thisWeekToggle, monthToggle}) {
            t.setToggleGroup(presets);
            t.getStyleClass().add("pnp-toggle");
            t.setMaxWidth(Double.MAX_VALUE);
        }
        todayToggle.setId("presetToday");
        weekAheadToggle.setId("presetWeekAhead");
        thisWeekToggle.setId("presetThisWeek");
        monthToggle.setId("presetMonth");
        todayToggle.setOnAction(e -> selectPreset(todayToggle));
        weekAheadToggle.setOnAction(e -> selectPreset(weekAheadToggle));
        thisWeekToggle.setOnAction(e -> selectPreset(thisWeekToggle));
        monthToggle.setOnAction(e -> selectPreset(monthToggle));
        // a toggle that is clicked twice must stay selected
        presets.selectedToggleProperty().addListener((obs, old, now) -> {
            if (now == null && old != null) {
                presets.selectToggle(old);
            }
        });

        Label fromLabel = new Label("From");
        fromLabel.getStyleClass().add("pnp-muted");
        Label toLabel = new Label("To");
        toLabel.getStyleClass().add("pnp-muted");
        fromPicker.setId("fromPicker");
        toPicker.setId("toPicker");
        fromPicker.setEditable(false);
        toPicker.setEditable(false);
        fromPicker.setMaxWidth(Double.MAX_VALUE);
        toPicker.setMaxWidth(Double.MAX_VALUE);
        fromPicker.valueProperty().addListener((obs, o, n) -> customDatesChanged());
        toPicker.valueProperty().addListener((obs, o, n) -> customDatesChanged());

        Label actionsTitle = new Label("Your list");
        actionsTitle.getStyleClass().add("pnp-section");
        copyButton.setId("copyList");
        saveButton.setId("saveList");
        uncheckButton.setId("uncheckAll");
        copyButton.getStyleClass().add("pnp-button");
        saveButton.getStyleClass().add("pnp-button-secondary");
        uncheckButton.getStyleClass().add("pnp-button-secondary");
        for (Button b : new Button[] {copyButton, saveButton, uncheckButton}) {
            b.setMaxWidth(Double.MAX_VALUE);
            b.setDisable(true);
        }
        copyButton.setOnAction(e -> copyList());
        saveButton.setOnAction(e -> saveList());
        uncheckButton.setOnAction(e -> {
            checked.clear();
            if (current != null) {
                drawList();
            }
        });

        VBox card = new VBox(8, rangeTitle, todayToggle, weekAheadToggle, thisWeekToggle, monthToggle,
                new VBox(2, fromLabel, fromPicker), new VBox(2, toLabel, toPicker), actionsTitle, copyButton, saveButton, uncheckButton);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildListCard() {
        summaryLabel.getStyleClass().add("pnp-section");
        summaryLabel.setId("shopSummary");
        boughtBar.getStyleClass().add("pnp-progress");
        boughtBar.setMaxWidth(Double.MAX_VALUE);
        boughtBar.setId("boughtBar");
        HBox.setHgrow(boughtBar, Priority.ALWAYS);
        boughtLabel.getStyleClass().add("pnp-muted");
        boughtLabel.setId("boughtLabel");
        boughtLabel.setMinWidth(Region.USE_PREF_SIZE);
        HBox progress = new HBox(10, boughtBar, boughtLabel);
        progress.setAlignment(Pos.CENTER_LEFT);

        missingBox.getStyleClass().add("pnp-warning");
        missingBox.setVisible(false);
        missingBox.setManaged(false);

        ScrollPane scroll = new ScrollPane(listBox);
        scroll.setFitToWidth(true);
        scroll.getStyleClass().add("pnp-scroll");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox card = new VBox(10, summaryLabel, progress, missingBox, scroll);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    // ------------------------------------------------------------------ choosing the days

    private void selectPreset(ToggleButton toggle) {
        presets.selectToggle(toggle);
        LocalDate today = todayProvider.get();
        LocalDate from = today;
        LocalDate to = today;
        if (toggle == weekAheadToggle) {
            to = today.plusDays(6);
        } else if (toggle == thisWeekToggle) {
            from = today.with(DayOfWeek.MONDAY);
            to = today.with(DayOfWeek.SUNDAY);
        } else if (toggle == monthToggle) {
            YearMonth month = YearMonth.from(today);
            from = month.atDay(1);
            to = month.atEndOfMonth();
        }
        updatingDates = true;
        fromPicker.setValue(from);
        toPicker.setValue(to);
        updatingDates = false;
        refresh();
    }

    private void customDatesChanged() {
        if (updatingDates) {
            return;
        }
        presets.selectToggle(null);
        refresh();
    }

    // ------------------------------------------------------------------ showing the list

    private void show(ShoppingList list, LocalDate from, LocalDate to) {
        current = list;
        copyButton.setDisable(list.isEmpty());
        saveButton.setDisable(list.isEmpty());
        uncheckButton.setDisable(list.isEmpty());
        String range = from.equals(to) ? DateTimeText.shortDate(from)
                : DateTimeText.shortDate(from) + " to " + DateTimeText.shortDate(to);
        summaryLabel.setText(list.getMealCount() + (list.getMealCount() == 1 ? " meal" : " meals") + " planned, "
                + list.itemCount() + (list.itemCount() == 1 ? " item" : " items") + " to buy   (" + range + ")");
        drawMissing(list);
        drawList();
        Theme.status(statusLabel, list.getMealCount() == 0
                ? "No meals are planned in these days. Add recipes in the Recipes tab."
                : "List is ready. Tick items as you buy them.", list.getMealCount() == 0 ? Theme.Kind.INFO : Theme.Kind.OK);
    }

    private void drawList() {
        listBox.getChildren().clear();
        if (current.isEmpty()) {
            Label none = new Label(current.getMealCount() == 0
                    ? "Nothing to buy yet.\nPlan some meals, then come back here."
                    : "These meals have no ingredient lists yet.\nUse \"Look up online\" above.");
            none.getStyleClass().add("pnp-empty");
            listBox.getChildren().add(none);
            updateProgress();
            return;
        }
        for (ShoppingList.Section section : current.getSections()) {
            Label head = new Label(section.getAisle().label());
            head.getStyleClass().add("pnp-aisle-header");
            Label count = Theme.chip(String.valueOf(section.getItems().size()), "chip-neutral");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox headRow = new HBox(8, head, count, spacer);
            headRow.setAlignment(Pos.CENTER_LEFT);

            VBox rows = new VBox(6, headRow);
            for (ShoppingList.Item item : section.getItems()) {
                rows.getChildren().add(itemRow(item));
            }
            rows.setPadding(new Insets(0, 0, 4, 0));
            listBox.getChildren().add(rows);
        }
        updateProgress();
    }

    private HBox itemRow(ShoppingList.Item item) {
        CheckBox box = new CheckBox(item.getName());
        box.getStyleClass().add("pnp-check");
        box.setSelected(checked.contains(item.getKey()));
        applyDone(box, box.isSelected());
        box.selectedProperty().addListener((obs, was, now) -> {
            if (now) {
                checked.add(item.getKey());
            } else {
                checked.remove(item.getKey());
            }
            applyDone(box, now);
            updateProgress();
        });
        Label note = new Label(item.note());
        note.getStyleClass().add("pnp-item-note");
        Tooltip.install(note, new Tooltip("Used in: " + String.join(", ", item.getRecipes())));
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox row = new HBox(10, box, spacer, note);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(0, 0, 0, 6));
        return row;
    }

    private static void applyDone(CheckBox box, boolean done) {
        box.getStyleClass().remove("pnp-check-done");
        if (done) {
            box.getStyleClass().add("pnp-check-done");
        }
    }

    private void updateProgress() {
        int total = current == null ? 0 : current.itemCount();
        int bought = 0;
        if (current != null) {
            for (ShoppingList.Section s : current.getSections()) {
                for (ShoppingList.Item i : s.getItems()) {
                    if (checked.contains(i.getKey())) {
                        bought++;
                    }
                }
            }
        }
        boughtBar.setProgress(total == 0 ? 0 : (double) bought / total);
        boughtLabel.setText(bought + " of " + total + " bought");
    }

    private void drawMissing(ShoppingList list) {
        missingBox.getChildren().clear();
        boolean any = !list.getMissingRecipes().isEmpty();
        missingBox.setVisible(any);
        missingBox.setManaged(any);
        if (!any) {
            return;
        }
        Label text = new Label("No ingredient list yet for: " + String.join(", ", list.getMissingRecipes()));
        text.setWrapText(true);
        Button lookup = new Button("Look up online");
        lookup.setId("lookupOnline");
        lookup.getStyleClass().add("pnp-button-secondary");
        lookup.disableProperty().bind(busy);
        lookup.setOnAction(e -> lookUpOnline(list.getMissingRecipes()));
        missingBox.getChildren().addAll(text, lookup);
    }

    // ------------------------------------------------------------------ actions

    private void lookUpOnline(java.util.List<String> names) {
        busy.set(true);
        Theme.status(statusLabel, "Looking up " + names.size() + " recipe(s) on TheMealDB...", Theme.Kind.INFO);
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return IngredientLookup.fill(store, client, names);
            }
        };
        task.setOnSucceeded(e -> {
            int found = task.getValue();
            busy.set(false);
            refresh();
            Theme.status(statusLabel, found == 0
                    ? "TheMealDB did not know these recipes. Add them from the Recipes tab instead."
                    : "Found ingredients for " + found + " of " + names.size() + " recipe(s).",
                    found == 0 ? Theme.Kind.ERROR : Theme.Kind.OK);
        });
        task.setOnFailed(e -> {
            busy.set(false);
            Theme.status(statusLabel, "Could not reach TheMealDB. Check your internet connection.", Theme.Kind.ERROR);
        });
        runInBackground(task);
    }

    private String title() {
        LocalDate from = fromPicker.getValue();
        LocalDate to = toPicker.getValue();
        return "Shopping list: " + DateTimeText.shortDate(from) + (from.equals(to) ? "" : " to " + DateTimeText.shortDate(to));
    }

    private void copyList() {
        ClipboardContent content = new ClipboardContent();
        content.putString(current.toPlainText(title(), checked));
        Clipboard.getSystemClipboard().setContent(content);
        Theme.status(statusLabel, "Copied! Paste it into a message or a note.", Theme.Kind.OK);
    }

    private void saveList() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save shopping list");
        chooser.setInitialFileName("shopping-list.txt");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text files", "*.txt"));
        File file = chooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file == null) {
            return;
        }
        String text = current.toPlainText(title(), checked);
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                Files.writeString(file.toPath(), text, StandardCharsets.UTF_8);
                return null;
            }
        };
        task.setOnSucceeded(e -> Theme.status(statusLabel, "Saved to " + file.getName(), Theme.Kind.OK));
        task.setOnFailed(e -> Theme.status(statusLabel, "Could not save the file: " + message(task.getException()), Theme.Kind.ERROR));
        runInBackground(task);
    }

    private static String message(Throwable t) {
        return t == null ? "unknown error" : t.getMessage();
    }

    private static void runInBackground(Task<?> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
