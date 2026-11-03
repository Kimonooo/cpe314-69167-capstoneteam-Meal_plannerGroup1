package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * QA tests that run the REAL sample CSV files through parser + analyzer.
 * (Maven runs tests from the project root, so "sample_data/..." resolves.)
 */
class SampleDataPipelineTest {

    private static List<ImportRow> load(String file, int[] badCount) throws IOException {
        List<String> lines = Files.readAllLines(Path.of("sample_data", file));
        List<ImportRow> good = new ArrayList<>();
        for (String line : lines.subList(1, lines.size())) { // skip header
            try {
                good.add(CsvRecipeParser.parseLine(line));
            } catch (IllegalArgumentException e) {
                badCount[0]++;
            }
        }
        return good;
    }

    private static List<String> categoriesOf(List<ImportRow> rows) {
        List<String> out = new ArrayList<>();
        for (ImportRow r : rows) {
            out.add(r.getCategory());
        }
        return out;
    }

    @Test
    void badRowsFileKeepsOnlyTheTwoGoodRows() throws IOException {
        int[] bad = new int[1];
        List<ImportRow> good = load("meal_history_bad_rows.csv", bad);
        assertEquals(2, good.size());
        assertEquals(5, bad[0]);
    }

    @Test
    void smallFileMinesToTheExpectedCounts() throws IOException {
        List<ImportRow> good = load("meal_history_small.csv", new int[1]);
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(categoriesOf(good));
        assertEquals(1, counts.get("Breakfast"));
        assertEquals(2, counts.get("MainCourse"));
        assertEquals(2, counts.get("Dessert"));
        assertEquals("Dessert", MealPlanAnalyzer.mostFrequent(counts)); // tie -> alphabetical
    }

    @Test
    void largeFileHas3000GoodRowsAndNoBadOnes() throws IOException {
        int[] bad = new int[1];
        List<ImportRow> good = load("meal_history_3000.csv", bad);
        assertEquals(3000, good.size());
        assertEquals(0, bad[0]);
    }

    @Test
    void addingRowsChangesTheMiningResult() throws IOException {
        List<String> cats = categoriesOf(load("meal_history_small.csv", new int[1]));
        cats.add("Breakfast");
        cats.add("Breakfast");
        cats.add("Breakfast");
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(cats);
        assertEquals(4, counts.get("Breakfast"));
        assertEquals("Breakfast", MealPlanAnalyzer.mostFrequent(counts));
    }
}
