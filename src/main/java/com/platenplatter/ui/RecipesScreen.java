package com.platenplatter.ui;

import com.platenplatter.api.ApiResult;
import com.platenplatter.api.MealData;
import com.platenplatter.controller.InvalidInputException;
import com.platenplatter.controller.RecipeController;
import com.platenplatter.controller.RecipeController.Details;
import com.platenplatter.controller.RecipeController.LevelHint;
import com.platenplatter.controller.RecipeController.Picks;
import com.platenplatter.controller.UserMessages;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.RecommendationService;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * The Recipes page. Nothing is listed until the user searches, apart from Today's recommendations.
 * Every recipe card shows the meal's real photo from TheMealDB (strMealThumb), loaded in the
 * background by MealPhotos. Clicking a card opens the "Add to my plan" panel with a larger photo:
 * the ingredients and the numbered steps are always shown, the user types the prep time (the app
 * only suggests one), and the healthy level appears by itself as soon as the calories are typed.
 * Without internet, a red message with a Retry button appears under the search bar.
 *
 * Every handler calls one RecipeController method, inside a background Task when it waits for
 * the internet or the database. Only the NEWEST answer is shown: each search or picks request
 * gets a number, and an older answer that arrives late is thrown away. Enter in the search box
 * is ignored while a search runs, the same as the disabled Search button.
 */
public class RecipesScreen {
    private static final List<String> LEVEL_CHIPS =
            List.of("chip-neutral", "chip-healthy", "chip-moderate", "chip-indulgent");

    private final RecipeController controller;
    private Runnable onPlanChanged = () -> { };
    private Runnable lastAction = this::loadRecommendations;
    private String shownPick = "";
    private int listRequest;                              // the newest search or picks request
    private boolean searching;

    // left side: search, banner, cards
    private final VBox root = new VBox(14);
    private final TextField searchField = new TextField();
    private final Button searchButton = new Button("Search");
    private final Button clearButton = new Button("Clear");
    private final HBox banner = new HBox();
    private final Label bannerText = new Label();
    private final Label sectionTitle = new Label("Today's recommendations");
    private final Label pickTag = new Label();
    private final Label sectionNote = new Label();
    private final GridPane cardGrid = new GridPane();
    private final Label listStatus = new Label();
    private VBox selectedCard;
    private final Map<String, VBox> cardsById = new HashMap<>();
    private List<MealData> lastPicks = List.of();
    private MealSlot lastPickSlot;
    private BiConsumer<List<MealData>, String> onPicksLoaded = (picks, title) -> { };

    // right side: the Add to my plan panel
    private final VBox panel = new VBox(10);
    private final StackPane panelPhoto = new StackPane();
    private final Label recipeName = new Label("Pick a recipe");
    private final Label recipeMeta =
            new Label("Search, or click one of today's picks, to see its ingredients.");
    private final FlowPane ingredientChips = new FlowPane(5, 5);
    private final Label stepsTitle = new Label("STEPS");
    private final VBox stepsBox = new VBox(8);
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<MealSlot> slotBox = new ComboBox<>();
    private final TextField prepField = new TextField();
    private final Button useSuggestion = new Button();
    private final TextField caloriesField = new TextField();
    private final Label caloriesNote = new Label();
    private final ComboBox<String> levelBox = new ComboBox<>();
    private final Label levelChip = new Label();
    private final Label autoHint = new Label();
    private final Label howNote = new Label();
    private final Button addButton = new Button("Add to my plan");
    private final Label addStatus = new Label();
    private final VBox form = new VBox(10);
    private Details current;
    private MealData opening; // the recipe whose details are being loaded
    private MealSlot pendingSlot;

    public RecipesScreen(RecipeController controller) {
        this.controller = controller;
        VBox left = new VBox(14, buildSearchCard(), buildCardsArea());
        ScrollPane leftScroll = new ScrollPane(left);
        leftScroll.setFitToWidth(true);
        leftScroll.getStyleClass().add("pnp-scroll");
        HBox.setHgrow(leftScroll, Priority.ALWAYS);
        ScrollPane rightScroll = new ScrollPane(buildPanel());
        rightScroll.setFitToWidth(true);
        rightScroll.getStyleClass().add("pnp-scroll");
        rightScroll.setPrefWidth(380);
        rightScroll.setMinWidth(340);
        HBox columns = new HBox(18, leftScroll, rightScroll);
        VBox.setVgrow(columns, Priority.ALWAYS);
        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().add(columns);
        Theme.apply(root);
        loadRecommendations();
    }

    public Parent getView() {
        return root;
    }

    public void setOnPlanChanged(Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged;
    }

    /**
     * Who wants today's picks when they arrive (the Today page shows the first one as its featured
     * meal). The picks are handed over as they are, so TheMealDB is not asked a second time.
     */
    public void setOnPicksLoaded(BiConsumer<List<MealData>, String> onPicksLoaded) {
        this.onPicksLoaded = onPicksLoaded;
        if (!lastPicks.isEmpty()) {
            onPicksLoaded.accept(lastPicks, shownPick);          // they were already loaded
        }
    }

    /**
     * Opens one of today's picks in the panel, the same as clicking its card (Today's featured
     * meal).
     */
    public void openMeal(MealData meal) {
        open(meal, cardsById.get(meal.mealDbId()), lastPickSlot);
    }

    /** Used by Meal Plan's "Add a meal for this day". */
    public void setDate(LocalDate date) {
        datePicker.setValue(date);
    }

    /** Used by Today's "Add lunch": the next recipe opened is planned for that slot. */
    public void setSlot(MealSlot slot) {
        pendingSlot = slot;
        if (slot != null && current != null) {
            slotBox.setValue(slot);
        }
    }

    /**
     * When the page is opened again: new picks if the part of the day changed (and no search is
     * shown).
     */
    public void refreshPicks() {
        if (searchField.getText().isBlank()
                && !shownPick.equals(RecommendationService.pickFor(LocalDateTime.now()).title())) {
            loadRecommendations();
        }
    }

    // ------------------------------------------------------------------ building the page

    private VBox buildSearchCard() {
        searchField.setId("searchField");
        searchField.setPromptText("For example adobo, penne or curry");
        searchField.getStyleClass().addAll("pnp-field", "pnp-search");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> search()); // Enter searches too (ignored while busy)
        searchButton.setId("searchButton");
        searchButton.getStyleClass().add("pnp-button");
        searchButton.setOnAction(e -> search());
        clearButton.setId("clearButton");
        clearButton.getStyleClass().add("pnp-button-secondary");
        clearButton.setOnAction(e -> {
            searchField.clear();
            loadRecommendations();
        });
        Theme.show(clearButton, false);
        HBox searchRow = new HBox(10, searchField, searchButton, clearButton);
        searchRow.setAlignment(Pos.CENTER_LEFT);
        Label searchLabel = Theme.text("Search recipes by name", "pnp-field-label");
        searchLabel.setLabelFor(searchField);

        bannerText.setId("offlineMessage");
        bannerText.getStyleClass().add("pnp-banner-text");
        bannerText.setWrapText(true);
        HBox.setHgrow(bannerText, Priority.ALWAYS);
        Button retry = new Button("Retry");
        retry.setId("retryButton");
        retry.getStyleClass().add("pnp-button");
        retry.setOnAction(e -> lastAction.run());
        banner.getChildren().addAll(bannerText, retry);
        banner.getStyleClass().add("pnp-banner");
        Theme.show(banner, false);

        VBox card = new VBox(8, searchLabel, searchRow, banner);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildCardsArea() {
        sectionTitle.getStyleClass().add("pnp-section");
        sectionTitle.setGraphic(Theme.icon("chef_hat", 24));
        sectionTitle.setGraphicTextGap(10);
        sectionTitle.setId("sectionTitle");
        pickTag.getStyleClass().addAll("pnp-chip", "chip-tag");
        pickTag.setId("pickTag");
        HBox titleRow = new HBox(8, sectionTitle, pickTag);
        titleRow.setAlignment(Pos.CENTER_LEFT);
        sectionNote.getStyleClass().add("pnp-muted");
        IntStream.range(0, 3).forEach(i -> {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(100.0 / 3);
            cardGrid.getColumnConstraints().add(column);
        });
        cardGrid.setHgap(12);
        cardGrid.setVgap(12);
        listStatus.setId("listStatus");
        listStatus.getStyleClass().add("pnp-empty");
        listStatus.setWrapText(true);
        return new VBox(8, titleRow, sectionNote, cardGrid, listStatus);
    }

    private VBox buildPanel() {
        recipeName.getStyleClass().add("pnp-section");
        recipeName.setId("recipeName");
        recipeName.setWrapText(true);
        recipeMeta.getStyleClass().add("pnp-muted");
        recipeMeta.setWrapText(true);
        stepsTitle.setId("stepsTitle");
        stepsTitle.getStyleClass().add("pnp-overline");
        stepsBox.setId("stepsBox");                             // always shown: no Show/Hide button
        stepsBox.getStyleClass().add("pnp-steps");
        buildFormInputs();
        form.getChildren().addAll(stepsTitle, stepsBox,
                Theme.text("PLAN THIS MEAL", "pnp-overline"),
                pair(field(null, "Date", datePicker), field(null, "Meal slot", slotBox)),
                pair(field(useSuggestion, "Prep time (min)", prepField),
                        field(caloriesNote, "Calories (kcal)", caloriesField)),
                field(levelChip, "Healthy level", levelBox), autoHint, howNote, addButton,
                        addStatus);
        Theme.show(form, false);
        Theme.show(panelPhoto, false);                          // shown when a recipe is opened
        panel.getChildren().addAll(Theme.text("ADD TO MY PLAN", "pnp-overline"), panelPhoto,
                recipeName, recipeMeta, ingredientChips, form);
        panel.getStyleClass().add("pnp-card");
        return panel;
    }

    /** The "Plan this meal" boxes, their notes and listeners, and the Add button. */
    private void buildFormInputs() {
        slotBox.setId("slotBox");
        slotBox.getItems().addAll(MealSlot.values());
        slotBox.setMaxWidth(Double.MAX_VALUE);
        datePicker.setId("datePicker");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        Theme.dateFormat(datePicker);                           // "Sat, 14 Nov 2026"
        prepField.setId("prepField");
        prepField.setPromptText("0 to 1440, e.g. 30");
        useSuggestion.setId("useSuggestion");
        useSuggestion.getStyleClass().addAll("pnp-link", "pnp-link-small");
        useSuggestion.setOnAction(e ->
                prepField.setText(String.valueOf(current.suggestedMinutes())));
        caloriesField.setId("caloriesField");
        caloriesField.setPromptText("0 to 10000, e.g. 520");
        Stream.of(prepField, caloriesField)
                .forEach(field -> field.getStyleClass().add("pnp-field"));
        levelBox.setId("levelBox");
        levelBox.getItems().addAll(HealthyLevel.HEALTHY.getLabel(),
                HealthyLevel.MODERATE.getLabel(),
                HealthyLevel.INDULGENT.getLabel());
        levelBox.setMaxWidth(Double.MAX_VALUE);
        levelChip.setId("levelChip");
        levelChip.getStyleClass().addAll("pnp-chip", "chip-neutral");
        autoHint.setId("autoHint");
        autoHint.setWrapText(true);
        autoHint.getStyleClass().add("pnp-muted");
        howNote.getStyleClass().add("pnp-note");
        howNote.setWrapText(true);
        caloriesNote.getStyleClass().add("pnp-hint");
        // the level follows the slot and the calories as they are typed; the user may still pick
        // another
        slotBox.valueProperty().addListener((obs, old, now) -> showAutoLevel());
        caloriesField.textProperty().addListener((obs, old, now) -> showAutoLevel());
        levelBox.valueProperty().addListener((obs, old, now) -> updateHint());
        prepField.textProperty().addListener((obs, old, now) -> updateHint());
        addButton.setId("addButton");
        addButton.getStyleClass().add("pnp-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addToPlan());
        addStatus.setId("addStatus");
        addStatus.setWrapText(true);
    }

    /** A small bold label (with an optional note, link or chip on the right) above an input box. */
    private static VBox field(Node note, String label, Region input) {
        Label name = Theme.text(label, "pnp-field-label");
        name.setLabelFor(input);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = note == null ? new HBox(name) : new HBox(4, name, gap, note);
        head.setAlignment(Pos.CENTER_LEFT);
        return new VBox(4, head, input);
    }

    private static HBox pair(VBox a, VBox b) {
        HBox.setHgrow(a, Priority.ALWAYS);
        HBox.setHgrow(b, Priority.ALWAYS);
        a.setPrefWidth(10);
        b.setPrefWidth(10);
        return new HBox(10, a, b);
    }

    // ------------------------------------------------------------------ recommendations and search

    private void loadRecommendations() {
        LocalDateTime now = LocalDateTime.now();
        RecommendationService.Pick pick = RecommendationService.pickFor(now);
        int mine = ++listRequest;                               // a search still running is now old
        shownPick = pick.title();
        lastAction = this::loadRecommendations;
        sectionTitle.setText("Today's recommendations");
        pickTag.setText(pick.title());
        Theme.show(pickTag, true);
        Theme.show(clearButton, false);
        String day = now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        sectionNote.setText("Picked for " + day + " " + partOfDay(now)
                + " from TheMealDB · the picks change tomorrow");
        listStatus.setText("Loading today's picks...");
        Task<Picks> task = new Task<>() {
            @Override
            protected Picks call() throws Exception {
                return controller.picks(now);
            }
        };
        task.setOnSucceeded(e -> {
            if (mine != listRequest) {
                return;                                         // the user searched meanwhile
            }
            ApiResult result = task.getValue().result();
            if (result.isFailed()) {
                showError(result.error(), "Today's picks need the internet.");
                return;
            }
            Theme.show(banner, false);
            List<MealData> picks = result.meals();
            showCards(picks, pick.slot());
            lastPicks = picks;
            lastPickSlot = pick.slot();
            onPicksLoaded.accept(picks, pick.title()); // Today shows its featured meal from these
            listStatus.setText(picks.isEmpty() ? "No picks today." : "");
        });
        task.setOnFailed(e -> showError(task.getException(), "Today's picks need the internet."));
        AppShell.runInBackground(task);
    }

    private void search() {
        if (searching) {
            return; // Enter while a search runs does nothing
        }
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            loadRecommendations();
            return;
        }
        int mine = ++listRequest;
        lastAction = this::search;
        setSearching(true);
        listStatus.setText("Searching...");
        Task<ApiResult> task = new Task<>() {
            @Override
            protected ApiResult call() throws Exception {
                return controller.search(text);
            }
        };
        task.setOnSucceeded(e -> {
            setSearching(false);
            if (mine != listRequest) {
                return; // an older answer: a newer one is shown
            }
            ApiResult result = task.getValue();
            if (result.isFailed()) {
                showError(result.error(), "");
                return;
            }
            Theme.show(banner, false);
            List<MealData> results = result.meals();
            sectionTitle.setText("Results for \"" + text + "\" (" + results.size() + ")");
            Theme.show(pickTag, false);
            Theme.show(clearButton, true);
            sectionNote.setText("Click a recipe to see its ingredients and plan it.");
            showCards(results, null);
            listStatus.setText(results.isEmpty() ? "No recipes found for \"" + text
                    + "\". Try another word, such as chicken or curry." : "");
        });
        task.setOnFailed(e -> {
            setSearching(false);
            if (mine == listRequest) {
                showError(task.getException(), "");
            }
        });
        AppShell.runInBackground(task);
    }

    private void setSearching(boolean busy) {
        searching = busy;
        searchButton.setDisable(busy);                          // no double searches
    }

    /** No internet, too many requests, or TheMealDB failed: the red message with Retry. */
    private void showError(Throwable error, String listText) {
        bannerText.setText(UserMessages.of(error));
        Theme.show(banner, true);
        cardGrid.getChildren().clear();
        listStatus.setText(listText);
    }

    private void showCards(List<MealData> meals, MealSlot slotHint) {
        cardGrid.getChildren().clear();
        cardsById.clear();
        selectedCard = null;
        meals.forEach(MealPhotos::remember); // the photo address came with the result
        IntStream.range(0, meals.size())
                .forEach(i -> cardGrid.add(recipeCard(meals.get(i), slotHint), i % 3, i / 3));
    }

    /** A card with the meal's photo on top, then the category, the name and the meal slot. */
    private VBox recipeCard(MealData meal, MealSlot slotHint) {
        Label name = Theme.text(meal.name(), "pnp-card-title");
        name.setWrapText(true);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        Label idea = Theme.text(slotHint == null ? "Recipe" : slotHint.getLabel() + " idea",
                "pnp-muted");
        HBox foot = new HBox(idea, gap, Theme.text("View recipe →", "pnp-overline"));
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        StackPane photo = MealPhotos.photo(meal.thumbnail(), meal.name(), 128);
        VBox.setMargin(photo, new Insets(0, 0, 6, 0));
        Label category = Theme.text(meal.category().toUpperCase(Locale.ROOT), "pnp-overline");
        VBox card = new VBox(4, photo, category, name, push, foot);
        card.getStyleClass().addAll("pnp-card", "pnp-recipe-card");
        card.setMinHeight(236);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> open(meal, card, slotHint));
        card.setFocusTraversable(true); // Tab reaches it; Enter or Space opens it
        card.setAccessibleText(meal.name() + ", " + meal.category()
                + ". Press Enter to view the recipe.");
        card.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER || e.getCode() == KeyCode.SPACE) {
                open(meal, card, slotHint);
            }
        });
        cardsById.put(meal.mealDbId(), card);
        return card;
    }

    // ------------------------------------------------------------------ the Add to my plan panel

    private void open(MealData meal, VBox card, MealSlot slotHint) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("selected");
        }
        selectedCard = card;
        if (card != null) { // null: opened from Today during a search
            card.getStyleClass().add("selected");
        }
        opening = meal; // an older, slower click must not win
        showPanelPhoto(meal);
        recipeName.setText(meal.name());
        recipeMeta.setText("Loading the recipe...");
        Theme.show(form, false);
        ingredientChips.getChildren().clear();
        Task<Details> task = new Task<>() {
            @Override
            protected Details call() throws Exception {
                return controller.open(meal);
            }
        };
        task.setOnSucceeded(e -> {
            if (meal == opening) {
                showDetails(task.getValue(), slotHint);
            }
        });
        task.setOnFailed(e -> {
            if (meal != opening) {
                return;
            }
            recipeMeta.setText("Could not open the recipe.");
            bannerText.setText(UserMessages.of(task.getException()));
            Theme.show(banner, true); // the cards stay; only the message appears
        });
        AppShell.runInBackground(task);
    }

    private void showDetails(Details details, MealSlot slotHint) {
        MealData meal = details.meal();
        current = details;
        MealPhotos.remember(meal);
        showPanelPhoto(meal);
        recipeName.setText(meal.name());
        recipeMeta.setText(RecipeController.about(meal));
        ingredientChips.getChildren().setAll(details.ingredients().stream()
                .map(label -> Theme.text(label, "pnp-ingredient")).toList());
        showSteps(details.steps());
        prepField.clear(); // no fixed prep time: the user types it
        useSuggestion.setText("Use " + details.suggestedMinutes() + " min");
        slotBox.setValue(RecipeController.slotFor(meal, pendingSlot, slotHint));
        pendingSlot = null;
        boolean known = details.usualCalories().isPresent();
        caloriesField.setText(known ? String.valueOf(details.usualCalories().getAsInt()) : "");
        caloriesNote.setText(known ? "from your history" : "type them once");
        howNote.setText(details.suggestionText());
        Theme.status(addStatus, "", Theme.Kind.INFO);
        Theme.show(form, true);
        showAutoLevel();
    }

    /** The larger photo at the top of the panel. The picture is already in memory from the card. */
    private void showPanelPhoto(MealData meal) {
        panelPhoto.getChildren().setAll(MealPhotos.photo(meal.thumbnail(), meal.name(), 176));
        Theme.show(panelPhoto, true);
    }

    /** The steps, numbered 1, 2, 3 ... and always visible under the ingredients. */
    private void showSteps(List<String> steps) {
        stepsTitle.setText(steps.isEmpty() ? "STEPS" : "STEPS (" + steps.size() + ")");
        if (steps.isEmpty()) {
            stepsBox.getChildren()
                    .setAll(Theme.text("This recipe has no written steps.", "pnp-empty"));
            return;
        }
        stepsBox.getChildren().setAll(IntStream.range(0, steps.size())
                .mapToObj(i -> stepRow(i + 1, steps.get(i)))
                .toList());
    }

    private static HBox stepRow(int number, String text) {
        Label badge = Theme.text(String.valueOf(number), "pnp-step-number");
        badge.setMinWidth(Region.USE_PREF_SIZE);
        Label body = new Label(text);
        body.setWrapText(true);
        HBox.setHgrow(body, Priority.ALWAYS);
        HBox row = new HBox(9, badge, body);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /**
     * Shows the healthy level by itself: every time the calories or the meal slot change, the
     * controller works the level out again and it is put in the box and on the colored chip.
     */
    private void showAutoLevel() {
        if (current == null || slotBox.getValue() == null) {
            return;
        }
        HealthyLevel auto = controller.autoLevel(current.meal(), slotBox.getValue(),
                caloriesField.getText());
        levelBox.setValue(auto.getLabel());
        updateHint();
    }

    /** The colored chip and the line under the boxes: the level and why, and the prep tier. */
    private void updateHint() {
        if (current == null || slotBox.getValue() == null) {
            return;
        }
        LevelHint hint = controller.levelHint(current.meal(), slotBox.getValue(),
                caloriesField.getText(), prepField.getText(), levelBox.getValue());
        levelChip.setText(hint.shown().getLabel());
        levelChip.getStyleClass().removeAll(LEVEL_CHIPS);
        levelChip.getStyleClass().add("chip-" + hint.shown().name().toLowerCase(Locale.ROOT));
        autoHint.setText(hint.text());
    }

    private void addToPlan() {
        PlannedMeal planned;
        try {
            planned = controller.buildMeal(current, datePicker.getValue(), slotBox.getValue(),
                    prepField.getText(), caloriesField.getText(), levelBox.getValue());
        } catch (InvalidInputException wrong) {
            Theme.status(addStatus, wrong.getMessage(), Theme.Kind.ERROR);   // nothing is saved
            return;
        }
        MealData meal = current.meal();
        addButton.setDisable(true);
        Task<Long> task = new Task<>() {
            @Override
            protected Long call() throws Exception {
                return controller.save(planned, meal);
            }
        };
        task.setOnSucceeded(e -> {
            addButton.setDisable(false);
            Theme.status(addStatus, "Added " + meal.name() + " to "
                    + planned.getMealSlot().getLabel()
                    + " on " + TextFormat.shortDate(planned.getPlanDate()) + ".", Theme.Kind.OK);
            if (planned.getCalories() > 0) {
                caloriesNote.setText("from your history");
            }
            onPlanChanged.run();
        });
        task.setOnFailed(e -> {
            addButton.setDisable(false);
            Theme.status(addStatus, UserMessages.of(task.getException()), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    private static String partOfDay(LocalDateTime now) {
        int hour = now.getHour();
        return hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
    }
}
