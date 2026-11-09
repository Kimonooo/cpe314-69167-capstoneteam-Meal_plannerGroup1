package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.platenplatter.api.MealData;
import com.platenplatter.model.MealSlot;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class RecommendationServiceTest {
    private static final LocalDate SATURDAY = LocalDate.of(2026, 11, 7);

    private static RecommendationService.Pick at(int hour, int minute) {
        return RecommendationService.pickFor(SATURDAY.atTime(hour, minute));
    }

    private static List<MealData> meals(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new MealData(String.valueOf(i), "Meal " + i, "Vegetarian", "", "", List.of())).toList();
    }

    @Test
    void beforeElevenIsBreakfast() {
        assertEquals(new RecommendationService.Pick(MealSlot.BREAKFAST, "Breakfast"), at(6, 0));
        assertEquals(MealSlot.BREAKFAST, at(10, 59).slot());
    }

    @Test
    void elevenToFourIsLunchAndTheCategoryRotatesByWeekday() {
        assertEquals(MealSlot.LUNCH, at(11, 0).slot());
        assertEquals(MealSlot.LUNCH, at(15, 59).slot());
        assertEquals("Vegetarian", at(13, 30).category());                 // Saturday: 6 % 4 = 2
        assertEquals("Lunch · Vegetarian", at(13, 30).title());
        assertEquals("Chicken", RecommendationService.pickFor(LocalDateTime.of(2026, 11, 5, 12, 0)).category()); // Thursday
    }

    @Test
    void fourToSixIsASnackAndSixOnwardIsDinner() {
        assertEquals(MealSlot.SNACK, at(16, 0).slot());
        assertEquals("Starter", at(17, 59).category());                     // 6 % 2 = 0
        assertEquals(MealSlot.DINNER, at(18, 0).slot());
        assertEquals("Lamb", at(23, 59).category());                        // 6 % 7 = 6
    }

    @Test
    void theSameDateGivesTheSameSixPicks() {
        List<MealData> all = meals(40);
        List<MealData> first = RecommendationService.todaysPicks(all, SATURDAY);
        assertEquals(6, first.size());
        assertEquals(first, RecommendationService.todaysPicks(all, SATURDAY));
        assertNotEquals(first, RecommendationService.todaysPicks(all, SATURDAY.plusDays(1)));
    }

    @Test
    void fewerThanSixMealsGivesAllOfThemAndTheListIsNotChanged() {
        List<MealData> few = meals(4);
        assertEquals(4, RecommendationService.todaysPicks(few, SATURDAY).size());
        assertEquals("Meal 1", few.get(0).name());
        assertEquals(0, RecommendationService.todaysPicks(List.of(), SATURDAY).size());
    }
}
