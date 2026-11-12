package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StatisticsService.Grouping;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class SampleDataPipelineTest {

    /** Every data line of a sample file, parsed; a bad line becomes an empty Optional. */
    private static List<Optional<PlannedMeal>> load(String file) throws IOException {
        return Files.readAllLines(Path.of("sample_data", file)).stream().skip(1).map(line -> {
            try {
                return Optional.of(CsvRecipeParser.parse(line));
            } catch (IllegalArgumentException bad) {
                return Optional.<PlannedMeal>empty();
            }
        }).toList();
    }

    private static List<PlannedMeal> good(List<Optional<PlannedMeal>> rows) {
        return rows.stream().flatMap(Optional::stream).toList();
    }

    @Test
    void badRowsFileKeepsOnlyTheTwoGoodRows() throws IOException {
        List<Optional<PlannedMeal>> rows = load("meal_history_bad_rows.csv");
        assertEquals(2, good(rows).size());
        assertEquals(5, rows.size() - good(rows).size());
        assertOldWindowsEncodingIsRead();
    }

    /** Part of the bad-rows test: a file that is not UTF-8 must be read, not make the whole import fail. */
    private static void assertOldWindowsEncodingIsRead() throws IOException {
        // "Pi\u00F1a colada" in Windows-1252 (one byte, 0xF1) is not valid UTF-8; the import must not fail on it
        Path excel = Files.createTempFile("excel", ".csv");
        try {
            byte[] header = "date,recipe_name,category,prep_minutes\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
            byte[] row = "2026-11-03,Pi\u00F1a colada,Dessert,10\n".getBytes(java.nio.charset.Charset.forName("windows-1252"));
            Files.write(excel, java.nio.ByteBuffer.allocate(header.length + row.length).put(header).put(row).array());
            List<String> lines = CsvRecipeParser.readLines(excel);
            assertEquals(2, lines.size());
            assertEquals("Pi\u00F1a colada", CsvRecipeParser.parse(lines.get(1)).getRecipeName());
        } finally {
            Files.deleteIfExists(excel);
        }
        // a normal UTF-8 file with a byte-order mark: the mark is removed and the line count is the same as before
        Path utf8 = Files.createTempFile("utf8", ".csv");
        try {
            Files.writeString(utf8, "\uFEFFdate,recipe_name,category,prep_minutes\n2026-11-03,Pi\u00F1a colada,Dessert,10\n");
            List<String> lines = CsvRecipeParser.readLines(utf8);
            assertEquals("date,recipe_name,category,prep_minutes", lines.get(0));
            assertEquals("Pi\u00F1a colada", CsvRecipeParser.parse(lines.get(1)).getRecipeName());
        } finally {
            Files.deleteIfExists(utf8);
        }
        assertEquals(Files.readAllLines(Path.of("sample_data", "meal_history_demo.csv")),
                CsvRecipeParser.readLines(Path.of("sample_data", "meal_history_demo.csv")));
    }

    @Test
    void largeFileHas3000GoodRowsAndNoBadOnes() throws IOException {
        List<Optional<PlannedMeal>> rows = load("meal_history_3000.csv");
        assertEquals(3000, good(rows).size());
        assertEquals(3000, rows.size());
    }

    @Test
    void theDemoFileMinesToTheNumbersInTheDesignDocument() throws IOException {
        List<PlannedMeal> meals = good(load("meal_history_demo.csv"));
        LocalDate monday = LocalDate.of(2026, 11, 2);
        List<PlannedMeal> week = meals.stream().filter(m -> !m.getPlanDate().isAfter(monday.plusDays(6))).toList();
        assertEquals(48, meals.size());
        assertEquals(24, week.size());
        assertEquals(Map.of("Healthy", 14L, "Moderate", 6L, "Indulgent", 4L),
                StatisticsService.totals(week, Grouping.HEALTHY_LEVEL));
        assertEquals(532.5, StatisticsService.averageCalories(week), 0.001);
        assertEquals(37.5, StatisticsService.averagePrepMinutes(week), 0.001);
    }

    @Test
    void addingMealsChangesTheMiningResult() throws IOException {
        List<PlannedMeal> meals = good(load("meal_history_small.csv"));
        long before = StatisticsService.totals(meals, Grouping.MEAL_SLOT).get("Breakfast");
        List<PlannedMeal> more = Stream.concat(meals.stream(),
                Stream.of(CsvRecipeParser.parse("2026-11-07,Tapsilog,Breakfast,20"))).toList();
        assertEquals(before + 1, StatisticsService.totals(more, Grouping.MEAL_SLOT).get("Breakfast"));
    }
}
