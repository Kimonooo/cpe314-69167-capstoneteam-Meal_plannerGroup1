package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.service.StatisticsService;
import com.platenplatter.service.StatisticsService.Grouping;
import com.platenplatter.util.CsvRecipeParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** CSV file -> parser -> the 5 tables -> query -> mined numbers, the whole path of the Import and Analytics pages. */
class EndToEndPipelineTest {
    private Connection conn;
    private MealPlanDao dao;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        User kim = new UserDao(conn).insertUser(new User(0, "kim", "Kim", 20, "F", AuthService.SECURITY_QUESTIONS.get(2)),
                "secret1", "adobo");
        dao = new MealPlanDao(conn, kim.getUserId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    /** Imports a sample file like CsvImportTask does and returns how many rows were skipped. */
    private long importFile(String name) throws Exception {
        List<String> lines = Files.readAllLines(Path.of("sample_data", name));
        return lines.stream().skip(1).filter(line -> !save(line)).count();
    }

    private boolean save(String line) {
        try {
            dao.saveMeal(CsvRecipeParser.parse(line), List.of());
            return true;
        } catch (IllegalArgumentException badRow) {
            return false;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void theWeekTenFileGivesTheNumbersOfTheDesignDocument() throws Exception {
        assertEquals(0, importFile("meal_history_week10.csv"));
        LocalDate monday = LocalDate.of(2026, 11, 2);
        List<PlannedMeal> week = dao.mealsBetween(monday, monday.plusDays(6));
        Map<String, Map<String, Long>> table = StatisticsService.countByDay(week, monday, Grouping.HEALTHY_LEVEL);
        assertEquals(24, week.size());
        assertEquals(Map.of("Healthy", 14L, "Moderate", 6L, "Indulgent", 4L), StatisticsService.totals(week, Grouping.HEALTHY_LEVEL));
        assertEquals(Map.of("Healthy", 3L, "Moderate", 1L, "Indulgent", 0L), table.get("Tue 3"));
        assertEquals("58%", String.format("%.0f%%", StatisticsService.healthyShare(week) * 100));
        assertEquals(48, dao.allMeals().size());
    }

    @Test
    void badRowsAreSkippedAndNeverReachTheDatabase() throws Exception {
        assertEquals(5, importFile("meal_history_bad_rows.csv"));
        assertEquals(2, dao.allMeals().size());
    }

    @Test
    void theBigFileLoadsAllThreeThousandRows() throws Exception {
        assertEquals(0, importFile("meal_history_3000.csv"));
        assertEquals(3000, dao.allMeals().size());
    }

    @Test
    void addingMealsChangesTheMinedResult() throws Exception {
        importFile("meal_history_small.csv");
        long before = StatisticsService.totals(dao.allMeals(), Grouping.MEAL_SLOT).get("Breakfast");
        dao.saveMeal(CsvRecipeParser.parse("2026-11-07,Tapsilog,Breakfast,20"), List.of());
        dao.saveMeal(CsvRecipeParser.parse("2026-11-08,Champorado,Breakfast,25"), List.of());
        assertEquals(before + 2, StatisticsService.totals(dao.allMeals(), Grouping.MEAL_SLOT).get("Breakfast"));
    }

    @Test
    void anEmptyDatabaseGivesAnEmptyResultNotACrash() throws Exception {
        List<PlannedMeal> none = dao.allMeals();
        assertTrue(none.isEmpty());
        assertTrue(Double.isNaN(StatisticsService.healthyShare(none)));
        assertEquals(0L, StatisticsService.maxCount(StatisticsService.countByWeekday(none, Grouping.HEALTHY_LEVEL)));
    }
}
