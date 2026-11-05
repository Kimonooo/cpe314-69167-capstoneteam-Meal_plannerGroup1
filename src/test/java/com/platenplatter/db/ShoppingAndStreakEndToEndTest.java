package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.util.CsvRecipeParser;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.ShoppingList;
import com.platenplatter.util.ShoppingPlanner;
import com.platenplatter.util.StreakCalculator;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** QA tests for the shopping list and the streak, running on the REAL database code (in memory, no window). */
class ShoppingAndStreakEndToEndTest {
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

    private void importFile(String name) throws Exception {
        List<String> lines = Files.readAllLines(Path.of("sample_data", name));
        for (String line : lines.subList(1, lines.size())) {
            dao.insert(CsvRecipeParser.parseLine(line));
        }
    }

    @Test
    void anImportedWeekGivesAShoppingListWithNoMissingRecipes() throws Exception {
        importFile("meal_history_small.csv"); // 5 meals, 2 to 6 November 2026
        ShoppingList list = ShoppingPlanner.plan(dao, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 6));
        assertEquals(5, list.getMealCount());
        assertTrue(list.getMissingRecipes().isEmpty());
        assertTrue(list.itemCount() > 15);
        assertFalse(list.getSections().isEmpty());
    }

    @Test
    void aOneDayRangeOnlyUsesThatDaysMeals() throws Exception {
        importFile("meal_history_small.csv");
        ShoppingList list = ShoppingPlanner.plan(dao, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3));
        assertEquals(1, list.getMealCount()); // Chicken Adobo only
    }

    @Test
    void ingredientsSavedFromTheRecipesTabBeatTheBuiltInOnes() throws Exception {
        dao.addMeal(new ImportRow("2026-11-03", "Chicken Adobo", "MainCourse", 45), List.of("Chicken", "Truffle oil"));
        ShoppingList list = ShoppingPlanner.plan(dao, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3));
        assertEquals(2, list.itemCount());
    }

    @Test
    void aMealRemovedFromThePlanDisappearsFromTheList() throws Exception {
        dao.insert(new ImportRow("2026-11-03", "Chicken Adobo", "MainCourse", 45));
        long id = dao.mealsOn(LocalDate.of(2026, 11, 3)).get(0).getId();
        dao.deleteMeal(id);
        ShoppingList list = ShoppingPlanner.plan(dao, LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3));
        assertTrue(list.isEmpty());
        assertEquals(0, list.getMealCount());
    }

    @Test
    void theStreakIsCountedFromTheDatabaseDates() throws Exception {
        LocalDate today = LocalDate.of(2026, 11, 4);
        dao.insert(new ImportRow(today.toString(), "A", "Dessert", 5));
        dao.insert(new ImportRow(today.minusDays(1).toString(), "B", "Dessert", 5));
        dao.insert(new ImportRow(today.minusDays(2).toString(), "C", "Dessert", 5));
        dao.insert(new ImportRow(today.minusDays(5).toString(), "D", "Dessert", 5));
        assertEquals(3, StreakCalculator.current(dao.plannedDates(), today));
        assertEquals(3, StreakCalculator.longest(dao.plannedDates()));
    }

    @Test
    void theBigSampleFileHasAHugeBestStreakButNoCurrentOne() throws Exception {
        importFile("meal_history_3000.csv"); // meals from January to October 2026
        LocalDate today = LocalDate.of(2026, 11, 4);
        assertEquals(0, StreakCalculator.current(dao.plannedDates(), today));
        assertTrue(StreakCalculator.longest(dao.plannedDates()) > 50);
    }
}
