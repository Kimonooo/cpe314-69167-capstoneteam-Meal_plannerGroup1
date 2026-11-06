package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SampleIngredientsTest {

    @Test
    void knownDishesHaveIngredientsAndCapitalsDoNotMatter() {
        assertFalse(SampleIngredients.forRecipe("Chicken Adobo").isEmpty());
        assertEquals(SampleIngredients.forRecipe("Chicken Adobo"), SampleIngredients.forRecipe("  chicken ADOBO "));
    }

    @Test
    void unknownOrNullGivesAnEmptyList() {
        assertTrue(SampleIngredients.forRecipe("Moon Soup").isEmpty());
        assertTrue(SampleIngredients.forRecipe(null).isEmpty());
    }

    @Test
    void everyDishInTheSampleCsvFilesHasIngredients() throws IOException {
        Set<String> dishes = new HashSet<>();
        for (String file : List.of("meal_history_3000.csv", "meal_history_30000.csv", "meal_history_small.csv")) {
            List<String> lines = Files.readAllLines(Path.of("sample_data", file));
            for (String line : lines.subList(1, lines.size())) {
                dishes.add(CsvRecipeParser.parseLine(line).getRecipeName());
            }
        }
        assertTrue(dishes.size() >= 20, "expected about 24 dishes but found " + dishes.size());
        for (String dish : dishes) {
            assertFalse(SampleIngredients.forRecipe(dish).isEmpty(), "no built-in ingredients for: " + dish);
        }
    }
}
