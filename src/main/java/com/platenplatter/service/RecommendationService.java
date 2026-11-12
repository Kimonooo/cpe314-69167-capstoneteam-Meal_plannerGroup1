package com.platenplatter.service;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;
import com.platenplatter.model.MealSlot;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Today's recommendations on the Recipes page: six recipes picked for the time of day.
 *   before 11:00     Breakfast
 *   11:00 to 15:59   Lunch:  Chicken, Seafood, Vegetarian or Pasta (rotates by weekday)
 *   16:00 to 17:59   Snack:  Starter or Side
 *   18:00 onward     Dinner: Beef, Chicken, Seafood, Vegetarian, Pork, Pasta or Lamb
 * The six are chosen with a shuffle seeded by the date: the same six all day, new ones tomorrow.
 */
public class RecommendationService {
    public static final int PICKS = 6;

    /** The meal slot and the TheMealDB category for a time of day. */
    public record Pick(MealSlot slot, String category) {
        public String title() {
            return slot.getLabel().equalsIgnoreCase(category) ? slot.getLabel() : slot.getLabel() + " · " + category;
        }
    }

    private static final List<String> LUNCH = List.of("Chicken", "Seafood",
            "Vegetarian", "Pasta");
    private static final List<String> SNACK = List.of("Starter", "Side");
    private static final List<String> DINNER = List.of("Beef", "Chicken", "Seafood",
            "Vegetarian", "Pork", "Pasta", "Lamb");

    private final MealDbClient client;
    private final Map<String, List<MealData>> cache = new ConcurrentHashMap<>();

    public RecommendationService(MealDbClient client) {
        this.client = client;
    }

    public static Pick pickFor(LocalDateTime now) {
        int hour = now.getHour();
        int day = now.getDayOfWeek().getValue();             // Monday 1 ... Sunday 7
        if (hour < 11) return new Pick(MealSlot.BREAKFAST, "Breakfast");
        if (hour < 16) return new Pick(MealSlot.LUNCH, LUNCH.get(day % LUNCH.size()));
        if (hour < 18) return new Pick(MealSlot.SNACK, SNACK.get(day % SNACK.size()));
        return new Pick(MealSlot.DINNER, DINNER.get(day % DINNER.size()));
    }

    public static List<MealData> todaysPicks(List<MealData> meals, LocalDate today) {
        List<MealData> copy = new ArrayList<>(meals);
        Collections.shuffle(copy, new Random(today.toEpochDay()));  // same all day
        return copy.stream().limit(PICKS).toList();
    }

    /**
     * The six picks for this moment. They are fetched once per part of the day and kept in memory,
     * so moving between pages does not call TheMealDB again. Call on a background thread.
     */
    public List<MealData> recommend(LocalDateTime now) throws IOException, InterruptedException {
        Pick pick = pickFor(now);
        String key = now.toLocalDate() + " " + pick.title();
        List<MealData> cached = cache.get(key);
        if (cached != null) {
            return cached;
        }
        List<MealData> picks = todaysPicks(client.listByCategory(pick.category()), now.toLocalDate());
        cache.put(key, picks);
        return picks;
    }
}
