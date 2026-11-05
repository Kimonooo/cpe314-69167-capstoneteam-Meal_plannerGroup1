package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class IngredientLookupTest {

    private static MealDbClient fake(List<MealData> answer) {
        return new MealDbClient() {
            @Override
            public List<MealData> searchByName(String name) {
                return answer;
            }
        };
    }

    @Test
    void anExactNameMatchIsSaved() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        MealDbClient client = fake(List.of(new MealData("1", "Teriyaki Chicken", "Chicken", "Japanese", "x", List.of("Chicken", "Rice"))));
        assertEquals(1, IngredientLookup.fill(store, client, List.of("teriyaki chicken")));
        assertEquals(List.of("Chicken", "Rice"), store.ingredientsFor("Teriyaki Chicken"));
    }

    @Test
    void aDifferentDishIsNeverSaved() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        MealDbClient client = fake(List.of(new MealData("2", "Chicken Curry", "Chicken", "Indian", "x", List.of("Chicken"))));
        assertEquals(0, IngredientLookup.fill(store, client, List.of("Chicken Soup")));
        assertTrue(store.ingredientsFor("Chicken Soup").isEmpty());
    }

    @Test
    void noResultsMeansNothingFound() throws Exception {
        assertEquals(0, IngredientLookup.fill(new InMemoryPlanStore(), fake(List.of()), List.of("Anything")));
    }

    @Test
    void aNetworkErrorIsPassedOnToTheCaller() {
        MealDbClient broken = new MealDbClient() {
            @Override
            public List<MealData> searchByName(String name) throws IOException {
                throw new IOException("offline");
            }
        };
        assertThrows(IOException.class, () -> IngredientLookup.fill(new InMemoryPlanStore(), broken, List.of("X")));
    }
}
