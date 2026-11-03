package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.util.ImportRow;
import com.platenplatter.util.MealPlanAnalyzer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MealPlanHistoryDaoTest {
    private Connection conn;
    private MealPlanHistoryDao dao;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite::memory:");
        dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    @Test
    void insertThenCountAndRead() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "Chicken Adobo", "MainCourse", 45));
        dao.insert(new ImportRow("2026-11-04", "Leche Flan", "Dessert", 50));
        assertEquals(2, dao.count());
        assertEquals(List.of("MainCourse", "Dessert"), dao.getPlannedCategories());
        assertEquals(List.of(45, 50), dao.getPrepMinutes());
    }

    @Test
    void emptyTableGivesEmptyResults() throws SQLException {
        assertEquals(0, dao.count());
        assertTrue(dao.getPlannedCategories().isEmpty());
    }

    @Test
    void findByDateReturnsOnlyThatDateAndUnknownDateIsEmpty() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "Chicken Adobo", "MainCourse", 45));
        dao.insert(new ImportRow("2026-11-04", "Leche Flan", "Dessert", 50));
        List<ImportRow> day = dao.findByDate("2026-11-04");
        assertEquals(1, day.size());
        assertEquals("Leche Flan", day.get(0).getRecipeName());
        assertTrue(dao.findByDate("2030-01-01").isEmpty());
    }

    @Test
    void negativePrepMinutesIsRejectedByTheDatabase() {
        assertThrows(SQLException.class,
                () -> dao.insert(new ImportRow("2026-11-03", "Bad Row", "Dessert", -5)));
    }

    @Test
    void changingTheDataChangesTheMinedResult() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "Tapsilog", "Breakfast", 20));
        dao.insert(new ImportRow("2026-11-04", "Chicken Adobo", "MainCourse", 45));
        dao.insert(new ImportRow("2026-11-05", "Pancit Canton", "MainCourse", 30));
        Map<String, Integer> before = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        assertEquals("MainCourse", MealPlanAnalyzer.mostFrequent(before));

        dao.insert(new ImportRow("2026-11-06", "Halo-Halo", "Dessert", 15));
        dao.insert(new ImportRow("2026-11-07", "Leche Flan", "Dessert", 50));
        dao.insert(new ImportRow("2026-11-08", "Buko Pandan", "Dessert", 20));
        Map<String, Integer> after = MealPlanAnalyzer.countByCategory(dao.getPlannedCategories());
        assertEquals("Dessert", MealPlanAnalyzer.mostFrequent(after));
    }

    @Test
    void deleteAllEmptiesTheTable() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "Tapsilog", "Breakfast", 20));
        dao.deleteAll();
        assertEquals(0, dao.count());
    }

    @Test
    void countByCategoryCountsOnlyThatCategory() throws SQLException { 
        dao.insert(new ImportRow("2026-11-03", "Tapsilog", "Breakfast", 20));
        dao.insert(new ImportRow("2026-11-04", "Leche Flan", "Dessert", 50));
        dao.insert(new ImportRow("2026-11-05", "Halo-Halo", "Dessert", 15));
        assertEquals(2, dao.countByCategory("Dessert"));
        assertEquals(0, dao.countByCategory("Nothing"));
    }
}
