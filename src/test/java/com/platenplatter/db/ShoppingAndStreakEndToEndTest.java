package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import com.platenplatter.util.CsvRecipeParser;
import com.platenplatter.util.ShoppingList;
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

class ShoppingAndStreakEndToEndTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 4);

    private Connection conn;
    private MealPlanDao dao;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        User kim = new UserDao(conn).insertUser(new User(0, "kim", "Kim", 20, "F", AuthService.SECURITY_QUESTIONS.get(3)),
                "secret1", "ucmain");
        dao = new MealPlanDao(conn, kim.getUserId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private void plan(String line, List<IngredientAmount> ingredients) throws Exception {
        dao.saveMeal(CsvRecipeParser.parse(line), ingredients);
    }

    @Test
    void anImportedWeekWithoutIngredientsListsEveryRecipeAsMissing() throws Exception {
        Files.readAllLines(Path.of("sample_data", "meal_history_small.csv")).stream().skip(1)
                .map(CsvRecipeParser::parse).forEach(meal -> {
                    try {
                        dao.saveMeal(meal, List.of());
                    } catch (SQLException e) {
                        throw new IllegalStateException(e);
                    }
                });
        LocalDate from = LocalDate.of(2026, 11, 2);
        LocalDate to = LocalDate.of(2026, 11, 6);
        assertTrue(dao.shoppingItems(from, to).isEmpty());
        assertEquals(5, ShoppingList.missingRecipes(dao.mealsBetween(from, to), dao.shoppingItems(from, to)).size());
    }

    @Test
    void theSameIngredientInTwoRecipesIsListedOnce() throws Exception {
        plan("2026-11-03,Chicken Adobo,MainCourse,45", List.of(new IngredientAmount("Garlic", "1 head"),
                new IngredientAmount("Chicken", "1 kg")));
        plan("2026-11-04,Garlic Rice,Breakfast,15", List.of(new IngredientAmount("garlic", "5 cloves")));
        var list = ShoppingList.build(dao.shoppingItems(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 4)));
        assertEquals(2, ShoppingList.count(list));
        assertTrue(ShoppingList.asText(list).contains("garlic (1 head, 5 cloves)"));
    }

    @Test
    void aOneDayRangeOnlyUsesThatDaysMeals() throws Exception {
        plan("2026-11-03,Chicken Adobo,MainCourse,45", List.of(new IngredientAmount("Chicken", "1 kg")));
        plan("2026-11-04,Leche Flan,Dessert,50", List.of(new IngredientAmount("Eggs", "10")));
        assertEquals(1, dao.shoppingItems(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)).size());
    }

    @Test
    void aMealRemovedFromThePlanDisappearsFromTheList() throws Exception {
        plan("2026-11-03,Chicken Adobo,MainCourse,45", List.of(new IngredientAmount("Chicken", "1 kg")));
        dao.deleteMeal(dao.mealsOn(LocalDate.of(2026, 11, 3)).get(0).getPlannedMealId());
        assertTrue(dao.shoppingItems(LocalDate.of(2026, 11, 3), LocalDate.of(2026, 11, 3)).isEmpty());
    }

    @Test
    void theStreakIsCountedFromTheDatabaseDates() throws Exception {
        plan(TODAY + ",A,Dessert,5", List.of());
        plan(TODAY.minusDays(1) + ",B,Dessert,5", List.of());
        plan(TODAY.minusDays(2) + ",C,Dessert,5", List.of());
        plan(TODAY.minusDays(5) + ",D,Dessert,5", List.of());
        assertEquals(3, StreakCalculator.current(dao.plannedDates(), TODAY));
        assertEquals(3, StreakCalculator.longest(dao.plannedDates()));
    }

    @Test
    void theBigSampleFileHasAHugeBestStreakButNoCurrentOne() throws Exception {
        Files.readAllLines(Path.of("sample_data", "meal_history_3000.csv")).stream().skip(1)
                .forEach(line -> {
                    try {
                        plan(line, List.of());
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                });
        assertEquals(0, StreakCalculator.current(dao.plannedDates(), TODAY));
        assertTrue(StreakCalculator.longest(dao.plannedDates()) > 50);
    }
}
