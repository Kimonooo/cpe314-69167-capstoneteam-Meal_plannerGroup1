package com.platenplatter.controller;

import com.platenplatter.api.ApiResult;
import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.api.MeasureParser;
import com.platenplatter.api.RecipeMapper;
import com.platenplatter.db.PlanStore;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.service.HealthClassifier;
import com.platenplatter.service.PrepTimeEstimator;
import com.platenplatter.service.RecommendationService;
import com.platenplatter.util.TextFormat;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.stream.Stream;

/**
 * The controller of the Recipes page. The page calls one method per user action:
 *   search / picks  ask TheMealDB (through MealDbClient) and return an OK, EMPTY or FAILED result;
 *   open            reads the full recipe, the calories used last time and the suggested prep time;
 *   levelHint       the healthy level for the calories typed so far, and why;
 *   buildMeal       checks what was typed (prep 0 to 1,440 min, calories 0 to 10,000) and builds
 *                   the PlannedMeal, or throws InvalidInputException with the sentence to show;
 *   save            saves the meal and its ingredients.
 * Methods that wait for the internet or the database are called from a background Task.
 */
public class RecipeController {

    /** Today's picks: which pick it is (slot and category) and TheMealDB's answer. */
    public record Picks(RecommendationService.Pick pick, ApiResult result) { }

    /** Everything the "Add to my plan" panel shows for one recipe. */
    public record Details(MealData meal, OptionalInt usualCalories, int basePrepMinutes,
                          int suggestedMinutes, String suggestionText, List<String> ingredients,
                          List<String> steps) { }

    /** The level the app works out, the level shown (the user may pick another), and why. */
    public record LevelHint(HealthyLevel auto, HealthyLevel shown, String text) { }

    private final MealDbClient client;
    private final RecommendationService recommendations;
    private final PlanStore store;

    public RecipeController(MealDbClient client, RecommendationService recommendations,
                            PlanStore store) {
        this.client = client;
        this.recommendations = recommendations;
        this.store = store;
    }

    /** Searches TheMealDB by name. Never throws for an internet problem: the result says FAILED. */
    public ApiResult search(String text) throws InterruptedException {
        return client.search(text == null ? "" : text.trim());
    }

    /** Today's six picks for this part of the day, each with the pick's category filled in. */
    public Picks picks(LocalDateTime now) throws InterruptedException {
        RecommendationService.Pick pick = RecommendationService.pickFor(now);
        try {
            List<MealData> meals = recommendations.recommend(now).stream()
                    // filter.php has none
                    .map(meal -> RecipeMapper.withCategory(meal, pick.category()))
                    .toList();
            return new Picks(pick, ApiResult.of(meals));
        } catch (IOException e) {
            return new Picks(pick, ApiResult.failed(e));
        }
    }

    /** The full recipe (looked up when only a summary is known) and what the panel needs for it. */
    public Details open(MealData meal) throws IOException, InterruptedException, SQLException {
        MealData full = meal.isSummary() ? client.lookupById(meal.mealDbId()).orElse(meal) : meal;
        OptionalInt usual = store.usualCalories(full.name());
        int base = PrepTimeEstimator.estimateBaseMinutes(full.instructions(),
                full.ingredients().size());
        SavedRecipe recipe = RecipeMapper.toRecipe(full, base, 0);
        int suggested = recipe.getPrepMinutes();                // the subclass adds its own time
        int extra = suggested - base;
        String text = "Type how long this meal takes you. The app only suggests " + suggested
                + " min (" + PrepTimeEstimator.cookingMinutes(full.instructions())
                + " min of cooking found in the steps + " + PrepTimeEstimator.MINUTES_PER_INGREDIENT
                + " min × " + full.ingredients().size() + " ingredients"
                + (extra == 0 ? "" : " + " + extra + " min "
                        + recipe.getMealTypeLabel().toLowerCase(Locale.ROOT) + " setup")
                + "). TheMealDB has no calories, so the app remembers the ones you enter "
                + "for each recipe.";
        return new Details(full, usual, base, suggested, text, ingredientLabels(full),
                TextFormat.steps(full.instructions()));
    }

    /** "200g Chicken", or "Salt (amount not given)" when TheMealDB gives no measure. */
    public static List<String> ingredientLabels(MealData meal) {
        return meal.ingredients().stream().map(RecipeController::ingredientLabel).toList();
    }

    private static String ingredientLabel(IngredientAmount amount) {
        String name = amount.getIngredient().getIngredientName();
        MeasureParser.Measure measure = MeasureParser.parse(amount.getMeasure());
        return measure.text().isBlank() ? name + " (" + MeasureParser.NOT_GIVEN + ")"
                : measure.text() + " " + name;
    }

    /** "Category · Area · 9 ingredients" under the recipe name. */
    public static String about(MealData meal) {
        return Stream.of(meal.category(), meal.area(), meal.ingredients().size() + " ingredients")
                .filter(s -> !s.isBlank()).reduce((a, b) -> a + " · " + b).orElse("");
    }

    /** The meal slot a recipe opens in: the one asked for, else the pick's, else its category's. */
    public static MealSlot slotFor(MealData meal, MealSlot asked, MealSlot hint) {
        return asked != null ? asked : hint != null ? hint : MealSlot.defaultFor(meal.category());
    }

    /**
     * The healthy level for the calories typed so far: worked out again on every key press.
     * levelText is the level shown in the box; when it differs, the user chose it.
     */
    public LevelHint levelHint(MealData meal, MealSlot slot, String caloriesText, String prepText,
                               String levelText) {
        int calories = Math.max(number(caloriesText), 0);
        HealthyLevel auto = HealthClassifier.classify(calories, slot, meal.category());
        HealthyLevel shown = HealthyLevel.fromTextOr(levelText, auto);
        String why;
        if (shown != auto) {
            why = "Your own choice. The app would say " + auto.getLabel() + ".";
        } else if (calories > 0) {
            why = shown.getLabel() + ": " + HealthClassifier.explain(calories, slot,
                    meal.category()) + ".";
        } else {
            why = "No calories yet, so the level follows the recipe type. "
                    + "Type the calories and it updates by itself.";
        }
        int prep = number(prepText);
        String tier = prep < 0 || prep > PlannedMeal.MAX_PREP_MINUTES ? ""
                : " Prep tier: " + PrepTier.of(prep).getLabel() + " ("
                        + PrepTier.of(prep).getRange() + ").";
        return new LevelHint(auto, shown, why + tier);
    }

    /** The level that follows the calories and the slot (used when the box is set by the app). */
    public HealthyLevel autoLevel(MealData meal, MealSlot slot, String caloriesText) {
        return HealthClassifier.classify(Math.max(number(caloriesText), 0), slot, meal.category());
    }

    /**
     * Checks everything typed on the panel and builds the meal to save.
     * @throws InvalidInputException with the sentence to show next to the Add button
     */
    public PlannedMeal buildMeal(Details details, LocalDate date, MealSlot slot, String prepText,
                                 String caloriesText,
                                         String levelText) throws InvalidInputException {
        if (details == null || date == null || slot == null) {
            throw new InvalidInputException("Pick a recipe, a date and a meal slot first.");
        }
        if (prepText == null || prepText.isBlank()) {
            throw new InvalidInputException("Type the prep time in minutes, or press \"Use "
                    + details.suggestedMinutes() + " min\".");
        }
        int prep = number(prepText);
        if (prep < 0 || prep > PlannedMeal.MAX_PREP_MINUTES) {
            throw new InvalidInputException("Prep time must be a whole number from 0 to "
                    + String.format(Locale.ENGLISH, "%,d", PlannedMeal.MAX_PREP_MINUTES)
                            + " minutes.");
        }
        int calories = caloriesText == null || caloriesText.isBlank() ? 0 : number(caloriesText);
        if (calories < 0 || calories > PlannedMeal.MAX_CALORIES) {
            throw new InvalidInputException("Calories must be a whole number from 0 to "
                    + String.format(Locale.ENGLISH, "%,d", PlannedMeal.MAX_CALORIES)
                    + ", or leave the box empty.");
        }
        MealData meal = details.meal();
        HealthyLevel level = HealthyLevel.fromTextOr(levelText,
                HealthClassifier.classify(calories, slot, meal.category()));
        SavedRecipe recipe = RecipeMapper.toRecipe(meal, details.basePrepMinutes(), calories);
        return new PlannedMeal(0, 0, recipe, date, slot, prep, calories, level);
    }

    /** Saves the meal and the recipe's ingredients (call it in a Task). */
    public long save(PlannedMeal meal, MealData from) throws SQLException {
        return store.saveMeal(meal, from.ingredients());
    }

    /** A whole number typed in a box, or -1 when it is not one. */
    static int number(String text) {
        try {
            return Integer.parseInt(text == null ? "" : text.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
