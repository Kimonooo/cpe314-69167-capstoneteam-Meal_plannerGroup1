package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.User;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.PlannedMeal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * QA tests for the new columns (meal slot, calories, healthy level), for "every user only sees their
 * own meals", and for upgrading a database file that an older build made. In memory, no window.
 */
class UserMealsAndUpgradeTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 3);
    private Connection conn;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private int addUser(String username) throws SQLException {
        UserDao users = new UserDao(conn);
        users.insertUser(new User(0, username, username, 20, "F", "hash", "salt"));
        return users.findByUsername(username).getId();
    }

    private static ImportRow lunch(String name, int calories) {
        return new ImportRow(DAY.toString(), name, "MainCourse", 30, MealSlot.LUNCH, calories, null);
    }

    @Test
    void theSlotCaloriesAndLevelAreSavedAndReadBack() throws SQLException {
        MealPlanHistoryDao dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
        dao.insert(new ImportRow(DAY.toString(), "Chicken Adobo", "MainCourse", 45, MealSlot.DINNER, 620, HealthyLevel.HEALTHY));
        PlannedMeal meal = dao.mealsOn(DAY).get(0);
        assertEquals(MealSlot.DINNER, meal.getMealSlot());
        assertEquals(620, meal.getCalories());
        assertEquals(HealthyLevel.HEALTHY, meal.getHealthyLevel());
        assertEquals("Average", meal.getPrepTier().getLabel());
    }

    @Test
    void theAutomaticLevelIsSavedWhenNoneIsPicked() throws SQLException {
        MealPlanHistoryDao dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
        dao.insert(lunch("Big Lunch", 1100));
        assertEquals(HealthyLevel.INDULGENT, dao.mealsOn(DAY).get(0).getHealthyLevel());
    }

    @Test
    void negativeCaloriesAreRejectedByTheDatabase() throws SQLException {
        MealPlanHistoryDao dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
        assertThrows(SQLException.class, () -> dao.insert(lunch("Bad", -10)));
    }

    @Test
    void aDaysMealsComeBackInBreakfastLunchDinnerSnackOrder() throws SQLException {
        MealPlanHistoryDao dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
        dao.insert(new ImportRow(DAY.toString(), "Halo-Halo", "Dessert", 15, MealSlot.SNACK, 0, null));
        dao.insert(new ImportRow(DAY.toString(), "Adobo", "MainCourse", 45, MealSlot.DINNER, 0, null));
        dao.insert(new ImportRow(DAY.toString(), "Tapsilog", "Breakfast", 20, MealSlot.BREAKFAST, 0, null));
        dao.insert(lunch("Sinigang", 0));
        List<PlannedMeal> meals = dao.mealsOn(DAY);
        assertEquals(List.of("Tapsilog", "Sinigang", "Adobo", "Halo-Halo"),
                List.of(meals.get(0).getRecipeName(), meals.get(1).getRecipeName(),
                        meals.get(2).getRecipeName(), meals.get(3).getRecipeName()));
    }

    @Test
    void eachUserOnlySeesTheirOwnMeals() throws SQLException {
        new MealPlanHistoryDao(conn).initSchema();
        MealPlanHistoryDao ana = new MealPlanHistoryDao(conn, addUser("ana"));
        MealPlanHistoryDao ben = new MealPlanHistoryDao(conn, addUser("ben"));
        ana.insert(lunch("Ana's Lunch", 500));
        ben.insert(lunch("Ben's Lunch", 500));
        ben.insert(lunch("Ben's Second Lunch", 500));

        assertEquals(1, ana.count());
        assertEquals(2, ben.count());
        assertEquals("Ana's Lunch", ana.mealsOn(DAY).get(0).getRecipeName());
        assertEquals(1, ana.allMeals().size());
        assertEquals(1, ana.plannedDates().size());
        assertEquals(2, ben.getPlannedCategories().size());
        assertEquals(3, new MealPlanHistoryDao(conn).count()); // a DAO with no user sees every row
    }

    @Test
    void aUserCannotRemoveAnotherUsersMeal() throws SQLException {
        new MealPlanHistoryDao(conn).initSchema();
        MealPlanHistoryDao ana = new MealPlanHistoryDao(conn, addUser("ana"));
        MealPlanHistoryDao ben = new MealPlanHistoryDao(conn, addUser("ben"));
        ana.insert(lunch("Ana's Lunch", 500));
        long anasMeal = ana.mealsOn(DAY).get(0).getId();
        ben.deleteMeal(anasMeal);
        ben.deleteAll();
        assertEquals(1, ana.count());
    }

    @Test
    void aMealCannotBelongToAUserWhoDoesNotExist() throws SQLException {
        new MealPlanHistoryDao(conn).initSchema();
        MealPlanHistoryDao nobody = new MealPlanHistoryDao(conn, 12345);
        assertThrows(SQLException.class, () -> nobody.insert(lunch("Lost Lunch", 500))); // foreign key
    }

    @Test
    void theFirstUserToLogInGetsTheMealsSavedBeforeAccountsExisted() throws SQLException {
        MealPlanHistoryDao everyone = new MealPlanHistoryDao(conn);
        everyone.initSchema();
        everyone.insert(lunch("Old Lunch", 0));
        everyone.insert(lunch("Old Dinner", 0));
        MealPlanHistoryDao ana = new MealPlanHistoryDao(conn, addUser("ana"));
        assertEquals(0, ana.count());
        assertEquals(2, ana.claimUnownedMeals());
        assertEquals(2, ana.count());
        MealPlanHistoryDao ben = new MealPlanHistoryDao(conn, addUser("ben"));
        assertEquals(0, ben.claimUnownedMeals()); // nothing left to claim
        assertEquals(0, everyone.claimUnownedMeals()); // a DAO with no user never claims
    }

    @Test
    void anOldDatabaseFileIsUpgradedAndKeepsItsMeals() throws SQLException {
        // the exact meal_history an older build made: 5 columns, no user, slot, calories or level
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE meal_history (id INTEGER PRIMARY KEY AUTOINCREMENT, plan_date TEXT NOT NULL, "
                    + "recipe_name TEXT NOT NULL, category TEXT NOT NULL, prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0))");
            st.execute("INSERT INTO meal_history (plan_date, recipe_name, category, prep_minutes) VALUES "
                    + "('2026-11-03', 'Tapsilog', 'Breakfast', 20), ('2026-11-03', 'Leche Flan', 'Dessert', 50), "
                    + "('2026-11-03', 'Chicken Adobo', 'MainCourse', 45)");
        }
        MealPlanHistoryDao dao = new MealPlanHistoryDao(conn);
        dao.initSchema();
        dao.initSchema(); // running it again must change nothing

        assertEquals(3, dao.count());
        List<PlannedMeal> meals = dao.mealsOn(DAY);
        assertEquals("Tapsilog", meals.get(0).getRecipeName());
        assertEquals(MealSlot.BREAKFAST, meals.get(0).getMealSlot());
        assertEquals("Chicken Adobo", meals.get(1).getRecipeName());
        assertEquals(MealSlot.DINNER, meals.get(1).getMealSlot());
        assertEquals("Leche Flan", meals.get(2).getRecipeName());
        assertEquals(MealSlot.SNACK, meals.get(2).getMealSlot());
        assertEquals(HealthyLevel.INDULGENT, meals.get(2).getHealthyLevel()); // a dessert with no calories
        assertEquals(HealthyLevel.MODERATE, meals.get(1).getHealthyLevel());

        // the upgraded table enforces the new rules too
        assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO meal_history (plan_date, recipe_name, category, prep_minutes, meal_slot) VALUES (?, ?, ?, ?, ?)")) {
                ps.setString(1, "2026-11-04");
                ps.setString(2, "Brunch Thing");
                ps.setString(3, "MainCourse");
                ps.setInt(4, 10);
                ps.setString(5, "BRUNCH");
                ps.executeUpdate();
            }
        });
    }

    @Test
    void theUsersTableIsCreatedForAFreshDatabase() throws SQLException {
        new MealPlanHistoryDao(conn).initSchema();
        try (PreparedStatement ps = conn.prepareStatement("SELECT name FROM sqlite_master WHERE type = 'table' AND name = ?")) {
            ps.setString(1, "users");
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next());
            }
        }
    }
}
