package com.platenplatter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.api.ApiResult;
import com.platenplatter.api.FakeSender;
import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.api.MeasureParser;
import com.platenplatter.db.InMemoryPlanStore;
import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.RecommendationService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/** The rules behind "Add to my plan", checked without JavaFX and without the internet. */
class RecipeControllerTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 16);

    private final InMemoryPlanStore store = new InMemoryPlanStore();

    private RecipeController controller(FakeSender sender) {
        MealDbClient client = new MealDbClient(MealDbClient.BASE_URL, sender);
        return new RecipeController(client, new RecommendationService(client), store);
    }

    private RecipeController.Details flan() throws Exception {
        MealData flan = new MealData("1", "Leche Flan", "Dessert", "Filipino",
                "Steam for 30 minutes.", List.of());
        return controller(FakeSender.offline()).open(flan);
    }

    @Test
    void aGoodFormBuildsTheRightSubclass() throws Exception {
        PlannedMeal meal = controller(FakeSender.offline()).buildMeal(flan(), DAY, MealSlot.SNACK,
                "50", "350", null);
        assertInstanceOf(DessertRecipe.class, meal.getRecipe());
        assertEquals(50, meal.getPrepMinutes());
        assertEquals(350, meal.getCalories());
        // 350 is over 1.5 x the snack budget
        assertEquals(HealthyLevel.INDULGENT, meal.getHealthyLevel());
    }

    @Test
    void anEmptyOrBadPrepTimeIsRefusedWithASentence() throws Exception {
        RecipeController c = controller(FakeSender.offline());
        RecipeController.Details flan = flan();
        InvalidInputException empty = assertThrows(InvalidInputException.class,
                () -> c.buildMeal(flan, DAY, MealSlot.SNACK, " ", "", null));
        assertTrue(empty.getMessage().startsWith("Type the prep time"));
        assertThrows(InvalidInputException.class, () -> c.buildMeal(flan, DAY, MealSlot.SNACK,
                "abc", "", null));
        assertThrows(InvalidInputException.class, () -> c.buildMeal(flan, DAY, MealSlot.SNACK, "-5",
                "", null));
    }

    @Test
    void prepAndCaloriesHaveUpperLimits() throws Exception {
        RecipeController c = controller(FakeSender.offline());
        RecipeController.Details flan = flan();
        assertEquals(1440, c.buildMeal(flan, DAY, MealSlot.SNACK, "1440", "10000",
                null).getPrepMinutes());
        assertThrows(InvalidInputException.class, () -> c.buildMeal(flan, DAY, MealSlot.SNACK,
                "100000", "", null));
        InvalidInputException calories = assertThrows(InvalidInputException.class,
                () -> c.buildMeal(flan, DAY, MealSlot.SNACK, "30", "2000000000", null));
        assertTrue(calories.getMessage().contains("10,000"));
    }

    @Test
    void noRecipeOrNoDateIsRefused() throws Exception {
        RecipeController c = controller(FakeSender.offline());
        assertThrows(InvalidInputException.class, () -> c.buildMeal(null, DAY, MealSlot.SNACK, "30",
                "", null));
        assertThrows(InvalidInputException.class, () -> c.buildMeal(flan(), null, MealSlot.SNACK,
                "30", "", null));
    }

    @Test
    void theLevelHintFollowsTheCaloriesAndSaysWhenTheUserChoseAnother() throws Exception {
        RecipeController c = controller(FakeSender.offline());
        MealData adobo = new MealData("2", "Adobo", "Chicken", "Filipino", "", List.of());
        assertEquals(HealthyLevel.HEALTHY, c.levelHint(adobo, MealSlot.LUNCH, "600", "",
                null).shown());
        RecipeController.LevelHint own = c.levelHint(adobo, MealSlot.LUNCH, "600", "", "Indulgent");
        assertEquals(HealthyLevel.INDULGENT, own.shown());
        assertTrue(own.text().startsWith("Your own choice"));
    }

    @Test
    void aSearchUsesTheSavedAnswerAndABlankMeasureIsShownAsNotGiven() throws Exception {
        ApiResult result = controller(FakeSender.file("search_arrabiata.json")).search("Arrabiata");
        assertEquals(ApiResult.Status.OK, result.status());
        MealData fish = MealDbClient.parseMeals(FakeSender.read("lookup_blank_measure.json"))
                .get(0);
        assertTrue(RecipeController.ingredientLabels(fish).contains("Salt ("
                + MeasureParser.NOT_GIVEN + ")"));
        assertTrue(RecipeController.ingredientLabels(fish).contains("1 lb Red Snapper"));
    }

    @Test
    void savingAddsTheMealAndRemembersItsCalories() throws Exception {
        RecipeController c = controller(FakeSender.offline());
        RecipeController.Details flan = flan();
        c.save(c.buildMeal(flan, DAY, MealSlot.SNACK, "50", "350", null), flan.meal());
        assertEquals(1, store.mealsOn(DAY).size());
        assertEquals(350, c.open(flan.meal()).usualCalories().getAsInt());
    }
}
