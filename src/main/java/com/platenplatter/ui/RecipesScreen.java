package com.platenplatter.ui;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.service.RecommendationService;
import com.platenplatter.util.CategoryMapper;
import com.platenplatter.util.HealthClassifier;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PrepTimeEstimator;
import com.platenplatter.util.TextFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

/**
 * The Recipes page. Nothing is listed until the user searches, apart from Today's recommendations.
 * Recipe cards are text only (no food photos). Clicking a card opens the "Add to my plan" panel,
 * where the prep time is estimated and the calories are filled in from the user's history.
 * Without internet, a red message with a Retry button appears under the search bar.
 * Every call to TheMealDB and to the database runs on a background thread.
 */
public class RecipesScreen {
    private static final String OFFLINE = "No internet connection. Check your Wi-Fi, then press Retry.";
    private static final String NOT_RESPONDING = "TheMealDB is not responding right now. Try again in a minute.";
    private static final String AUTO = "Auto";

    /** A full recipe and the calories the user entered for it last time (if any). */
    private record Details(MealData meal, OptionalInt usualCalories) { }

    private final MealDbClient client;
    private final RecommendationService recommendations;
    private final PlanStore store;
    private Runnable onPlanChanged = () -> { };
    private Runnable lastAction = this::loadRecommendations;
    private String shownPick = "";

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

    // right side: the Add to my plan panel
    private final VBox panel = new VBox(10);
    private final Label recipeName = new Label("Pick a recipe");
    private final Label recipeMeta = new Label("Search, or click one of today's picks, to see its ingredients.");
    private final FlowPane ingredientChips = new FlowPane(5, 5);
    private final Label steps = new Label();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<MealSlot> slotBox = new ComboBox<>();
    private final TextField prepField = new TextField();
    private final Label prepNote = new Label("estimated");
    private final TextField caloriesField = new TextField();
    private final Label caloriesNote = new Label();
    private final ComboBox<String> levelBox = new ComboBox<>();
    private final Label autoHint = new Label();
    private final Label howNote = new Label();
    private final Button addButton = new Button("Add to my plan");
    private final Label addStatus = new Label();
    private final VBox form = new VBox(10);
    private MealData current;
    private int basePrepMinutes;
    private MealSlot pendingSlot;

    public RecipesScreen(MealDbClient client, RecommendationService recommendations, PlanStore store) {
        this.client = client;
        this.recommendations = recommendations;
        this.store = store;
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
        root.getChildren().addAll(Theme.pageHeader("Recipes",
                "Search TheMealDB by name. Nothing is listed until you search, except today's picks."), columns);
        Theme.apply(root);
        loadRecommendations();
    }

    public Parent getView() {
        return root;
    }

    public void setOnPlanChanged(Runnable onPlanChanged) {
        this.onPlanChanged = onPlanChanged;
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

    /** When the page is opened again: new picks if the part of the day changed (and no search is shown). */
    public void refreshPicks() {
        if (searchField.getText().isBlank()
                && !shownPick.equals(RecommendationService.pickFor(LocalDateTime.now()).title())) {
            loadRecommendations();
        }
    }

    // ------------------------------------------------------------------ building the page

    private VBox buildSearchCard() {
        searchField.setId("searchField");
        searchField.setPromptText("Search a recipe by name, e.g. adobo, penne, curry");
        searchField.getStyleClass().addAll("pnp-field", "pnp-search");
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.setOnAction(e -> search());              // Enter searches too
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

        VBox card = new VBox(10, searchRow, banner);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildCardsArea() {
        sectionTitle.getStyleClass().add("pnp-section");
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
        steps.setWrapText(true);
        steps.getStyleClass().add("pnp-muted");
        Button stepsToggle = new Button("Show steps");
        stepsToggle.getStyleClass().add("pnp-link");
        stepsToggle.setOnAction(e -> {
            Theme.show(steps, !steps.isVisible());
            stepsToggle.setText(steps.isVisible() ? "Hide steps" : "Show steps");
        });
        Theme.show(steps, false);

        slotBox.setId("slotBox");
        slotBox.getItems().addAll(MealSlot.values());
        slotBox.setMaxWidth(Double.MAX_VALUE);
        datePicker.setId("datePicker");
        datePicker.setMaxWidth(Double.MAX_VALUE);
        prepField.setId("prepField");
        caloriesField.setId("caloriesField");
        caloriesField.setPromptText("e.g. 520");
        Stream.of(prepField, caloriesField).forEach(field -> field.getStyleClass().add("pnp-field"));
        levelBox.setId("levelBox");
        levelBox.getItems().addAll(AUTO, HealthyLevel.HEALTHY.getLabel(), HealthyLevel.MODERATE.getLabel(),
                HealthyLevel.INDULGENT.getLabel());
        levelBox.setValue(AUTO);
        levelBox.setMaxWidth(Double.MAX_VALUE);
        autoHint.setId("autoHint");
        autoHint.setWrapText(true);
        autoHint.getStyleClass().add("pnp-muted");
        howNote.getStyleClass().add("pnp-note");
        howNote.setWrapText(true);
        prepNote.getStyleClass().add("pnp-muted");
        caloriesNote.getStyleClass().add("pnp-muted");
        slotBox.valueProperty().addListener((obs, old, now) -> updateHint());
        levelBox.valueProperty().addListener((obs, old, now) -> updateHint());
        caloriesField.textProperty().addListener((obs, old, now) -> updateHint());
        prepField.textProperty().addListener((obs, old, now) -> updateHint());
        addButton.setId("addButton");
        addButton.getStyleClass().add("pnp-button");
        addButton.setMaxWidth(Double.MAX_VALUE);
        addButton.setOnAction(e -> addToPlan());
        addStatus.setId("addStatus");
        addStatus.setWrapText(true);

        form.getChildren().addAll(stepsToggle, steps,
                pair(field("Date", datePicker), field("Meal slot", slotBox)),
                pair(field(prepNote, "Prep time (min)", prepField), field(caloriesNote, "Calories (kcal)", caloriesField)),
                field("Healthy level", levelBox), autoHint, howNote, addButton, addStatus);
        Theme.show(form, false);
        panel.getChildren().addAll(Theme.text("ADD TO MY PLAN", "pnp-overline"), recipeName, recipeMeta, ingredientChips, form);
        panel.getStyleClass().add("pnp-card");
        return panel;
    }

    private static VBox field(String label, Region input) {
        return field(null, label, input);
    }

    /** A small bold label (with an optional grey note on the right) above an input box. */
    private static VBox field(Label note, String label, Region input) {
        Label name = Theme.text(label, "pnp-muted");
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 11.5px;");
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox head = note == null ? new HBox(name) : new HBox(4, name, gap, note);
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
        shownPick = pick.title();
        lastAction = this::loadRecommendations;
        sectionTitle.setText("Today's recommendations");
        pickTag.setText(pick.title());
        Theme.show(pickTag, true);
        Theme.show(clearButton, false);
        sectionNote.setText("Picked for " + now.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + partOfDay(now) + " from TheMealDB · the picks change tomorrow");
        listStatus.setText("Loading today's picks...");
        Task<List<MealData>> task = new Task<>() {
            @Override
            protected List<MealData> call() throws Exception {
                return recommendations.recommend(now);
            }
        };
        task.setOnSucceeded(e -> {
            Theme.show(banner, false);
            List<MealData> picks = task.getValue().stream()          // filter.php has no category: use the pick's
                    .map(m -> new MealData(m.mealDbId(), m.name(), pick.category(), m.area(), m.instructions(), m.ingredients()))
                    .toList();
            showCards(picks, pick.slot());
            listStatus.setText(picks.isEmpty() ? "No picks today." : "");
        });
        task.setOnFailed(e -> showError(task.getException(), "Today's picks need the internet."));
        AppShell.runInBackground(task);
    }

    private void search() {
        String text = searchField.getText().trim();
        if (text.isEmpty()) {
            loadRecommendations();
            return;
        }
        lastAction = this::search;
        searchButton.setDisable(true);                          // no double searches
        listStatus.setText("Searching...");
        Task<List<MealData>> task = new Task<>() {
            @Override
            protected List<MealData> call() throws Exception {
                return client.searchByName(text);
            }
        };
        task.setOnSucceeded(e -> {
            searchButton.setDisable(false);
            Theme.show(banner, false);
            List<MealData> results = task.getValue();
            sectionTitle.setText("Results for \"" + text + "\" (" + results.size() + ")");
            Theme.show(pickTag, false);
            Theme.show(clearButton, true);
            sectionNote.setText("Click a recipe to see its ingredients and plan it.");
            showCards(results, null);
            listStatus.setText(results.isEmpty()
                    ? "No recipes found for \"" + text + "\". Try another word, such as chicken or curry." : "");
        });
        task.setOnFailed(e -> {
            searchButton.setDisable(false);
            showError(task.getException(), "");
        });
        AppShell.runInBackground(task);
    }

    /** No internet, or TheMealDB failed: the red message under the search bar, with Retry. */
    private void showError(Throwable error, String listText) {
        bannerText.setText(MealDbClient.isOffline(error) ? OFFLINE : NOT_RESPONDING);
        Theme.show(banner, true);
        cardGrid.getChildren().clear();
        listStatus.setText(listText);
    }

    private void showCards(List<MealData> meals, MealSlot slotHint) {
        cardGrid.getChildren().clear();
        selectedCard = null;
        IntStream.range(0, meals.size())
                .forEach(i -> cardGrid.add(recipeCard(meals.get(i), slotHint), i % 3, i / 3));
    }

    /** A text-only card: the category, the name and the meal slot. No photo is downloaded. */
    private VBox recipeCard(MealData meal, MealSlot slotHint) {
        Label name = Theme.text(meal.name(), "pnp-card-title");
        name.setWrapText(true);
        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);
        HBox foot = new HBox(Theme.text(slotHint == null ? "Recipe" : slotHint.getLabel() + " idea", "pnp-muted"), gap,
                Theme.text("View recipe →", "pnp-overline"));
        Region push = new Region();
        VBox.setVgrow(push, Priority.ALWAYS);
        VBox card = new VBox(4, Theme.text(meal.category().toUpperCase(Locale.ROOT), "pnp-overline"), name, push, foot);
        card.getStyleClass().addAll("pnp-card", "pnp-recipe-card");
        card.setMinHeight(108);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setOnMouseClicked(e -> open(meal, card, slotHint));
        return card;
    }

    // ------------------------------------------------------------------ the Add to my plan panel

    private void open(MealData meal, VBox card, MealSlot slotHint) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("selected");
        }
        selectedCard = card;
        card.getStyleClass().add("selected");
        recipeName.setText(meal.name());
        recipeMeta.setText("Loading the recipe...");
        Theme.show(form, false);
        ingredientChips.getChildren().clear();
        Task<Details> task = new Task<>() {
            @Override
            protected Details call() throws Exception {
                MealData full = meal.isSummary() ? client.lookupById(meal.mealDbId()).orElse(meal) : meal;
                return new Details(full, store.usualCalories(full.name()));
            }
        };
        task.setOnSucceeded(e -> showDetails(task.getValue(), slotHint));
        task.setOnFailed(e -> {
            recipeMeta.setText("Could not open the recipe.");
            showError(task.getException(), listStatus.getText());
        });
        AppShell.runInBackground(task);
    }

    private void showDetails(Details details, MealSlot slotHint) {
        MealData meal = details.meal();
        current = meal;
        recipeName.setText(meal.name());
        recipeMeta.setText(Stream.of(meal.category(), meal.area(), meal.ingredients().size() + " ingredients")
                .filter(s -> !s.isBlank()).reduce((a, b) -> a + " · " + b).orElse(""));
        ingredientChips.getChildren().setAll(meal.ingredients().stream()
                .map(amount -> Theme.text(amount.label(), "pnp-ingredient")).toList());
        steps.setText(meal.instructions());

        basePrepMinutes = PrepTimeEstimator.estimateBaseMinutes(meal.instructions(), meal.ingredients().size());
        SavedRecipe recipe = recipeFor(meal, 0);
        prepField.setText(String.valueOf(recipe.getPrepMinutes()));     // the subclass adds its own time
        prepNote.setText("estimated");
        MealSlot slot = pendingSlot != null ? pendingSlot : slotHint != null ? slotHint : MealSlot.defaultFor(meal.category());
        slotBox.setValue(slot);
        pendingSlot = null;
        caloriesField.setText(details.usualCalories().isPresent() ? String.valueOf(details.usualCalories().getAsInt()) : "");
        caloriesNote.setText(details.usualCalories().isPresent() ? "from your history" : "type them once");
        levelBox.setValue(AUTO);
        howNote.setText("Prep time = " + PrepTimeEstimator.cookingMinutes(meal.instructions())
                + " min of cooking found in the steps + " + PrepTimeEstimator.MINUTES_PER_INGREDIENT + " min × "
                + meal.ingredients().size() + " ingredients" + extraText(recipe) + ". TheMealDB has no calories, so the app"
                + " remembers the ones you enter for each recipe. You can change both.");
        Theme.status(addStatus, "", Theme.Kind.INFO);
        Theme.show(form, true);
        updateHint();
    }

    private static String extraText(SavedRecipe recipe) {
        int extra = recipe.getPrepMinutes() - recipe.getBasePrepMinutes();
        return extra == 0 ? "" : " + " + extra + " min " + recipe.getMealTypeLabel().toLowerCase() + " setup";
    }

    private SavedRecipe recipeFor(MealData meal, int calories) {
        return SavedRecipe.create(CategoryMapper.toMealType(meal.category()), 0, meal.name(), meal.area(),
                basePrepMinutes, calories, meal.mealDbId());
    }

    /** The line under the boxes: what Auto chose and why, and the prep tier. */
    private void updateHint() {
        if (current == null || slotBox.getValue() == null) {
            return;
        }
        int calories = number(caloriesField.getText());
        int prep = number(prepField.getText());
        HealthyLevel auto = HealthClassifier.classify(Math.max(calories, 0), slotBox.getValue(), current.category());
        String level = AUTO.equals(levelBox.getValue()) ? "Auto: " + auto.getLabel() : levelBox.getValue();
        String tier = prep < 0 ? "" : " Prep tier: " + PrepTier.of(prep).getLabel() + " (" + PrepTier.of(prep).getRange() + ").";
        autoHint.setText(level + " (" + HealthClassifier.explain(Math.max(calories, 0), slotBox.getValue(), current.category())
                + ")." + tier);
    }

    private void addToPlan() {
        int prep = number(prepField.getText());
        int calories = caloriesField.getText().isBlank() ? 0 : number(caloriesField.getText());
        if (current == null || datePicker.getValue() == null || slotBox.getValue() == null) {
            Theme.status(addStatus, "Pick a recipe, a date and a meal slot first.", Theme.Kind.ERROR);
            return;
        }
        if (prep < 0 || calories < 0) {
            Theme.status(addStatus, "Prep time and calories must be whole numbers, 0 or more.", Theme.Kind.ERROR);
            return;
        }
        MealSlot slot = slotBox.getValue();
        HealthyLevel level = AUTO.equals(levelBox.getValue())
                ? HealthClassifier.classify(calories, slot, current.category())
                : HealthyLevel.fromText(levelBox.getValue());
        MealData meal = current;
        PlannedMeal planned = new PlannedMeal(0, 0, recipeFor(meal, calories), datePicker.getValue(), slot, prep, calories, level);
        addButton.setDisable(true);
        Task<Long> task = new Task<>() {
            @Override
            protected Long call() throws Exception {
                return store.saveMeal(planned, meal.ingredients());
            }
        };
        task.setOnSucceeded(e -> {
            addButton.setDisable(false);
            Theme.status(addStatus, "Added " + meal.name() + " to " + slot.getLabel() + " on "
                    + TextFormat.shortDate(planned.getPlanDate()) + ".", Theme.Kind.OK);
            caloriesNote.setText(calories > 0 ? "from your history" : caloriesNote.getText());
            onPlanChanged.run();
        });
        task.setOnFailed(e -> {
            addButton.setDisable(false);
            Theme.status(addStatus, "Could not save: " + task.getException().getMessage(), Theme.Kind.ERROR);
        });
        AppShell.runInBackground(task);
    }

    /** A whole number typed in a box, or -1 when it is not one. */
    private static int number(String text) {
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static String partOfDay(LocalDateTime now) {
        int hour = now.getHour();
        return hour < 12 ? "morning" : hour < 18 ? "afternoon" : "evening";
    }
}
