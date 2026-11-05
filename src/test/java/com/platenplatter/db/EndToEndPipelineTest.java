package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.util.CsvRecipeParser;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.MealPlanAnalyzer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * QA tests for the WHOLE chain, with no window: CSV file -> parser -> database -> mining.
 * It uses a throw-away database that lives in memory, so it never touches platenplatter.db.
 */
class EndToEndPipelineTest {
    private Connection conn;
    private MealPlanHistoryDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    /** Reads a CSV the way the importer does: good rows go to the database, bad rows are counted. */
    private int importFile(String name) throws IOException, SQLException {
        List<String> lines = Files.readAllLines(Path.of("sample_data", name));
        int skipped = 0;
        for (String line : lines.subList(1, lines.size())) {
            try {
                dao.insert(CsvRecipeParser.parseLine(line));
            } catch (IllegalArgumentException bad) {
                skipped++;
            }
        }
        return skipped;
    }

    @Test
    void smallFileGivesTheExpectedChartNumbers() throws Exception {
        assertEquals(0, importFile("meal_history_small.csv"));
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        assertEquals(1, counts.get("Breakfast"));
        assertEquals(2, counts.get("MainCourse"));
        assertEquals(2, counts.get("Dessert"));
    }

    @Test
    void badRowsAreSkippedAndNeverReachTheDatabase() throws Exception {
        assertEquals(5, importFile("meal_history_bad_rows.csv"));
        assertEquals(2, dao.count());
    }

    @Test
    void theBigFileLoadsAllThreeThousandRows() throws Exception {
        assertEquals(0, importFile("meal_history_3000.csv"));
        assertEquals(3000, dao.count());
        assertEquals("MainCourse", MealPlanAnalyzer.mostFrequent(
                MealPlanAnalyzer.countByCategory(dao.getPlannedCategories())));
    }

    @Test
    void addingMealsChangesTheMinedResult() throws Exception {
        importFile("meal_history_small.csv");
        Map<String, Integer> before = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        for (int i = 0; i < 3; i++) {
            dao.insert(new ImportRow("2026-12-0" + (i + 1), "Pancakes", "Breakfast", 15));
        }
        Map<String, Integer> after = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        assertEquals(1, before.get("Breakfast"));
        assertEquals(4, after.get("Breakfast"));
        assertEquals("Breakfast", MealPlanAnalyzer.mostFrequent(after));
    }

    @Test
    void anEmptyDatabaseGivesAnEmptyResultNotACrash() throws Exception {
        Map<String, Integer> counts = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        assertTrue(counts.isEmpty());
        assertNull(MealPlanAnalyzer.mostFrequent(counts));
        assertEquals(0.0, MealPlanAnalyzer.averagePrepMinutes(dao.getPrepMinutes()), 0.0001);
    }
}
