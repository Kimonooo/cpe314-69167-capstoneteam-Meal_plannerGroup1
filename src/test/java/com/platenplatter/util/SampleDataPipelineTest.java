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
    }

    @Test
    void largeFileHas3000GoodRowsAndNoBadOnes() throws IOException {
        List<Optional<PlannedMeal>> rows = load("meal_history_3000.csv");
        assertEquals(3000, good(rows).size());
        assertEquals(3000, rows.size());
    }

    @Test
    void theWeekTenFileMinesToTheNumbersInTheDesignDocument() throws IOException {
        List<PlannedMeal> meals = good(load("meal_history_week10.csv"));
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
