package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * MealPlanDao.saveMeals: one transaction for a whole import, duplicates, Cancel, and the one lock.
 */
class BatchSaveTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private Connection conn;
    private UserDao users;
    private User kim;
    private MealPlanDao dao;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        users = new UserDao(conn);
        kim = users.insertUser(new User(0, "kim", "Kim", 20, "Female",
                AuthService.SECURITY_QUESTIONS.get(0)),
                "secret1", "bantay");
        dao = new MealPlanDao(conn, kim.getUserId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private static PlannedMeal meal(String name, int day) {
        return new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, name, "", 30, 500, ""),
                MONDAY.plusDays(day), MealSlot.DINNER, 30, 500, HealthyLevel.HEALTHY);
    }

    private static List<PlannedMeal> week() {
        return IntStream.range(0, 7).mapToObj(day -> meal("Adobo " + day, day)).toList();
    }

    @Test
    void aWholeBatchIsSavedAndCounted() throws Exception {
        PlanStore.BatchResult result = dao.saveMeals(week(), done -> true);
        assertEquals(7, result.saved());
        assertEquals(0, result.duplicates());
        assertEquals(7, dao.allMeals().size());
    }

    @Test
    void aMealAlreadyPlannedIsCountedNotSavedAgain() throws Exception {
        dao.saveMeals(week(), done -> true);
        PlanStore.BatchResult again = dao.saveMeals(week(), done -> true);    // the same file twice
        assertEquals(0, again.saved());
        assertEquals(7, again.duplicates());
        assertEquals(7, dao.allMeals().size());
    }

    @Test
    void theSameRecipeInAnotherSlotIsNotADuplicate() throws Exception {
        PlannedMeal dinner = meal("Adobo", 0);
        PlannedMeal lunch = new PlannedMeal(0, 0, dinner.getRecipe(), dinner.getPlanDate(),
                MealSlot.LUNCH,
                30, 500, HealthyLevel.HEALTHY);
        assertEquals(2, dao.saveMeals(List.of(dinner, lunch), done -> true).saved());
    }

    @Test
    void cancelRollsBackEveryMealOfTheBatch() throws Exception {
        assertThrows(CancellationException.class, () -> dao.saveMeals(week(), done -> done < 4));
        assertTrue(dao.allMeals().isEmpty()); // not even the first three are kept
        assertTrue(conn.getAutoCommit());                        // the connection is back to normal
    }

    @Test
    void aNewDatabaseRefusesPrepTimesOverOneDay() throws Exception {
        dao.saveMeal(meal("Adobo", 0), List.of());
        try (Statement st = conn.createStatement()) {
            assertThrows(SQLException.class,
                    () -> st.executeUpdate("UPDATE planned_meals SET prep_minutes = 100000"));
            assertThrows(SQLException.class,
                    () -> st.executeUpdate("UPDATE planned_meals SET calories = 2000000000"));
        }
    }

    /**
     * The bug from the Week 11 check: with one lock per DAO, a rename on another thread ran
     * INSIDE the import's open transaction and was rolled back with it. With one lock for the
     * connection, the rename waits until the import has finished.
     */
    @Test
    void aRenameDuringAnImportIsNotRolledBackWithTheImport() throws Exception {
        CountDownLatch importIsOpen = new CountDownLatch(1);
        Thread importer = new Thread(() -> {
            try {
                dao.saveMeals(week(), done -> {
                    if (done == 1) {
                        importIsOpen.countDown();
                        pause(300);                              // the rename is tried meanwhile
                    }
                    return done < 3;                              // then the user presses Cancel
                });
            } catch (CancellationException | SQLException expected) {
                // the import is rolled back
            }
        });
        importer.start();
        importIsOpen.await();
        assertTrue(users.updateDisplayName(kim.getUserId(), "Kimberly"));   // waits for the lock
        importer.join();
        assertEquals("Kimberly", users.findById(kim.getUserId()).orElseThrow().getDisplayName());
        assertTrue(dao.allMeals().isEmpty());
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
