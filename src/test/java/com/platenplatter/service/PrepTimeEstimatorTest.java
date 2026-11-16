package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.platenplatter.model.PrepTier;
import com.platenplatter.model.SavedRecipe;
import org.junit.jupiter.api.Test;

class PrepTimeEstimatorTest {

    /** The time phrases in TheMealDB's steps for Spicy Arrabiata Penne (8 ingredients). */
    private static final String ARRABIATA = "Bring a large pot of water to a boil. Add kosher salt to the boiling water, then add "
            + "the pasta. Cook according to the package instructions, about 9 minutes. In a large skillet over medium-high heat, "
            + "add the olive oil and heat until the oil starts to shimmer. Add the garlic and cook, stirring, until fragrant, "
            + "1 to 2 minutes. Add the chopped tomatoes, red chile flakes, Italian seasoning and salt and pepper to taste. "
            + "Bring to a boil and cook for 5 minutes. Remove from the heat and add the chopped basil.";

    @Test
    void theArrabiataStepsGiveABaseOfThirtyTwoMinutes() {
        assertEquals(16, PrepTimeEstimator.cookingMinutes(ARRABIATA));           // 9 + 2 + 5
        assertEquals(32, PrepTimeEstimator.estimateBaseMinutes(ARRABIATA, 8));   // 16 + 8 x 2
    }

    @Test
    void asAMainCourseItTakesFortyTwoMinutesWhichIsAverage() {
        int base = PrepTimeEstimator.estimateBaseMinutes(ARRABIATA, 8);
        SavedRecipe recipe = SavedRecipe.create("MAIN_COURSE", 0, "Spicy Arrabiata Penne", "Italian", base, 0, "52771");
        assertEquals(42, recipe.getPrepMinutes());
        assertEquals(PrepTier.AVERAGE, PrepTier.of(recipe.getPrepMinutes()));
    }

    @Test
    void aRangeCountsItsSecondNumberAndHoursCountSixty() {
        assertEquals(2, PrepTimeEstimator.cookingMinutes("Stir for 1 to 2 minutes."));
        assertEquals(15, PrepTimeEstimator.cookingMinutes("Bake 10-15 mins."));
        assertEquals(60, PrepTimeEstimator.cookingMinutes("Simmer for 1 hour."));
        assertEquals(150, PrepTimeEstimator.cookingMinutes("Braise 2 hrs, then rest 30 minutes."));
    }

    @Test
    void numbersThatAreNotTimesAreIgnored() {
        assertEquals(0, PrepTimeEstimator.cookingMinutes("Heat the oven to 180C and use 2 eggs."));
    }

    @Test
    void noTimesGiveTwoMinutesPerIngredientButAtLeastFive() {
        assertEquals(20, PrepTimeEstimator.estimateBaseMinutes("Mix everything.", 10));
        assertEquals(5, PrepTimeEstimator.estimateBaseMinutes("Mix.", 1));
        assertEquals(5, PrepTimeEstimator.estimateBaseMinutes(null, 0));
    }

    @Test
    void theEstimateIsNeverOverEightHours() {
        assertEquals(480, PrepTimeEstimator.estimateBaseMinutes("Marinate for 24 hours.", 5));
    }
}
