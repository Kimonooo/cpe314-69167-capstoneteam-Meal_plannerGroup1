package com.platenplatter.ui;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.SavedRecipe;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/**
 * The food photos of the app. Every photo comes from TheMealDB (the strMealThumb field of a meal);
 * no address is written in the code. A photo is a rounded box that first shows the first letter of
 * the meal's name on a dark rose background and then, when the picture has arrived, the picture
 * cropped to fill the box. If there is no picture, or no internet, the letter simply stays.
 *
 * Nothing here blocks the window: JavaFX downloads each Image on its own background thread, and
 * the look-up of a planned meal's photo runs in a Task. Each picture is downloaded once and each
 * meal is looked up once, then kept in memory. All methods are called on the JavaFX thread.
 */
public final class MealPhotos {
    /** Set this to false to run the app without food photos: every box then keeps its letter and nothing is downloaded. */
    public static final boolean SHOW_PHOTOS = true;

    private static final double CORNER = 10;
    private static final double LOAD_SIZE = 480;                                   // enough for the biggest box
    private static final Color PLACEHOLDER = Color.web("#4A2A38");

    private static final Map<String, Image> IMAGES = new HashMap<>();              // photo address -> picture
    private static final Map<String, String> ADDRESSES = new HashMap<>();          // mealdb_id -> photo address
    private static final Map<String, List<Consumer<String>>> WAITING = new HashMap<>();
    private static MealDbClient client;

    private MealPhotos() { }

    /** The one TheMealDB client of the app, used to look up the photo of a planned meal. */
    public static void useClient(MealDbClient mealDbClient) {
        client = mealDbClient;
    }

    /** Remembers the photo address of a meal already read from TheMealDB, so it is never asked for again. */
    public static void remember(MealData meal) {
        if (meal != null && meal.hasPhoto() && !meal.mealDbId().isBlank()) {
            ADDRESSES.put(meal.mealDbId(), meal.thumbnail());
        }
    }

    /** A photo box as wide as its parent and this many pixels high (recipe cards and the recipe panel). */
    public static StackPane photo(String address, String mealName, double height) {
        StackPane box = placeholder(mealName);
        box.setMinHeight(height);
        box.setPrefHeight(height);
        box.setMaxHeight(height);
        box.setMaxWidth(Double.MAX_VALUE);
        fill(box, address);
        return box;
    }

    /** A photo box of a fixed width and height (the featured meal on Today). */
    public static StackPane photo(String address, String mealName, double width, double height) {
        StackPane box = photo(address, mealName, height);
        box.setMinWidth(width);
        box.setPrefWidth(width);
        box.setMaxWidth(width);
        return box;
    }

    /**
     * A small square photo of a saved recipe (the rows of Today and Meal Plan). The database keeps
     * the recipe's mealdb_id, not its photo, so the address is looked up once with lookup.php.
     * A meal from a CSV import has no mealdb_id and keeps its letter.
     */
    public static StackPane thumbnail(SavedRecipe recipe, double size) {
        StackPane box = photo("", recipe.getRecipeName(), size, size);
        box.getChildren().forEach(letter -> letter.getStyleClass().add("pnp-photo-letter-small"));
        String id = recipe.getMealDbId() == null ? "" : recipe.getMealDbId().trim();
        if (!SHOW_PHOTOS) {
            return box;
        }
        if (ADDRESSES.containsKey(id)) {
            fill(box, ADDRESSES.get(id));
        } else if (!id.isEmpty() && client != null) {
            whenKnown(id, address -> fill(box, address));
        }
        return box;
    }

    /** Looks the photo address of a meal up once; everyone who asked meanwhile gets the same answer. */
    private static void whenKnown(String id, Consumer<String> use) {
        List<Consumer<String>> waiting = WAITING.get(id);
        if (waiting != null) {
            waiting.add(use);                                   // a look-up for this meal is already running
            return;
        }
        List<Consumer<String>> first = new ArrayList<>();
        first.add(use);
        WAITING.put(id, first);
        MealDbClient mealDb = client;
        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return mealDb.lookupById(id).map(MealData::thumbnail).orElse("");
            }
        };
        task.setOnSucceeded(e -> {
            String address = task.getValue();
            ADDRESSES.put(id, address);                         // "" is remembered too: this meal has no photo
            List<Consumer<String>> asked = WAITING.remove(id);
            if (asked != null) {
                asked.forEach(waiter -> waiter.accept(address));
            }
        });
        task.setOnFailed(e -> WAITING.remove(id));              // no internet: the letter stays, tried again later
        AppShell.runInBackground(task);
    }

    /** The box before (or without) a picture: the first letter of the meal on a dark rose background. */
    private static StackPane placeholder(String mealName) {
        String name = mealName == null ? "" : mealName.trim();
        Label letter = new Label(name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase());
        letter.getStyleClass().add("pnp-photo-letter");
        StackPane box = new StackPane(letter);
        box.setBackground(new Background(new BackgroundFill(PLACEHOLDER, new CornerRadii(CORNER), Insets.EMPTY)));
        Rectangle clip = new Rectangle();                       // rounds the corners of the picture too
        clip.setArcWidth(CORNER * 2);
        clip.setArcHeight(CORNER * 2);
        clip.widthProperty().bind(box.widthProperty());
        clip.heightProperty().bind(box.heightProperty());
        box.setClip(clip);
        return box;
    }

    /** Puts the picture in the box when it has arrived. The download runs on a JavaFX background thread. */
    private static void fill(StackPane box, String address) {
        if (!SHOW_PHOTOS || address == null || address.isBlank()) {
            return;
        }
        Image image = IMAGES.get(address);
        if (image == null || image.isError()) {
            image = new Image(address, LOAD_SIZE, LOAD_SIZE, true, true, true);   // true = load in the background
            IMAGES.put(address, image);
        }
        Image picture = image;
        if (picture.getProgress() >= 1 && !picture.isError()) {
            show(box, picture);
            return;
        }
        picture.progressProperty().addListener((obs, old, now) -> {
            if (now.doubleValue() >= 1 && !picture.isError()) {
                show(box, picture);                             // this listener runs on the JavaFX thread
            }
        });
    }

    /** The picture covers the whole box and is cut at the edges, so every card has the same shape. */
    private static void show(StackPane box, Image picture) {
        BackgroundSize cover = new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true);
        box.setBackground(new Background(new BackgroundImage(picture, BackgroundRepeat.NO_REPEAT,
                BackgroundRepeat.NO_REPEAT, BackgroundPosition.CENTER, cover)));
        box.getChildren().clear();                              // the letter is not needed any more
    }
}
