package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.util.ImportRow;
import com.platenplatter.util.MealPlanAnalyzer;
import com.platenplatter.util.PlannedMeal;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.time.LocalDate;
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
        assertTrue(dao.plannedDates().isEmpty());
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

    @Test
    void mealsBetweenIncludesBothEndDaysAndKeepsOrder() throws SQLException {
        dao.insert(new ImportRow("2026-11-05", "C", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-03", "A", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-04", "B", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-09", "Z", "Dessert", 5));
        List<PlannedMeal> list = dao.mealsBetween(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 5));
        assertEquals(3, list.size());
        assertEquals("A", list.get(0).getRecipeName());
        assertEquals("C", list.get(2).getRecipeName());
    }

    @Test
    void plannedDatesHasEachDayOnce() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "A", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-03", "B", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-04", "C", "Dessert", 5));
        assertEquals(2, dao.plannedDates().size());
        assertTrue(dao.plannedDates().contains(LocalDate.of(2026, 11, 4)));
    }

    @Test
    void deleteMealRemovesOnlyThatMeal() throws SQLException {
        dao.insert(new ImportRow("2026-11-03", "A", "Dessert", 5));
        dao.insert(new ImportRow("2026-11-03", "B", "Dessert", 5));
        long id = dao.mealsOn(LocalDate.of(2026, 11, 3)).get(0).getId();
        dao.deleteMeal(id);
        List<PlannedMeal> left = dao.mealsOn(LocalDate.of(2026, 11, 3));
        assertEquals(1, left.size());
        assertEquals("B", left.get(0).getRecipeName());
        dao.deleteMeal(99999); // deleting an id that does not exist is not an error
    }

    @Test
    void mealAndIngredientsAreSavedTogether() throws SQLException {
        dao.addMeal(new ImportRow("2026-11-03", "Chicken Adobo", "MainCourse", 45), List.of("Chicken", "Soy sauce", "Garlic"));
        assertEquals(1, dao.count());
        assertEquals(List.of("Chicken", "Garlic", "Soy sauce"), dao.ingredientsFor("chicken adobo"));
    }

    @Test
    void savingTheSameIngredientsTwiceDoesNotDuplicateThem() throws SQLException {
        dao.saveIngredients("Halo-Halo", List.of("Ice", "Milk"));
        dao.saveIngredients("Halo-Halo", List.of("Ice", "Milk", "Beans"));
        assertEquals(3, dao.ingredientsFor("Halo-Halo").size());
    }

    @Test
    void unknownRecipeHasNoIngredientsAndBlankOnesAreSkipped() throws SQLException {
        assertTrue(dao.ingredientsFor("Nothing").isEmpty());
        dao.saveIngredients("X", List.of("  ", "Rice"));
        assertEquals(List.of("Rice"), dao.ingredientsFor("X"));
    }

    @Test
    void ifTheMealCannotBeSavedNoIngredientsAreSavedEither() {
        assertThrows(SQLException.class, () -> dao.addMeal(new ImportRow("2026-11-03", "Bad", "Dessert", -1), List.of("Salt")));
        try {
            assertTrue(dao.ingredientsFor("Bad").isEmpty());
        } catch (SQLException e) {
            fail(e);
        }
    }
}
