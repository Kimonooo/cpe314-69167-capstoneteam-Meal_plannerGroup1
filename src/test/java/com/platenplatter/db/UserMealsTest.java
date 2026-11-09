package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserMealsTest {
    private static final LocalDate DAY = LocalDate.of(2026, 11, 3);

    private Connection conn;
    private UserDao users;
    private MealPlanDao kim;
    private MealPlanDao jon;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        users = new UserDao(conn);
        String question = AuthService.SECURITY_QUESTIONS.get(1);
        kim = new MealPlanDao(conn, users.insertUser(new User(0, "kim", "Kim", 20, "F", question), "secret1", "cebu").getUserId());
        jon = new MealPlanDao(conn, users.insertUser(new User(0, "jon", "Jon", 21, "M", question), "secret2", "davao").getUserId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private static PlannedMeal adobo() {
        return new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, "Chicken Adobo", "", 35, 620, ""), DAY,
                MealSlot.DINNER, 45, 620, HealthyLevel.HEALTHY);
    }

    @Test
    void eachUserOnlySeesTheirOwnMeals() throws Exception {
        kim.saveMeal(adobo(), List.of());
        assertEquals(1, kim.allMeals().size());
        assertTrue(jon.allMeals().isEmpty());
        assertTrue(jon.plannedDates().isEmpty());
        assertTrue(jon.mealsOn(DAY).isEmpty());
    }

    @Test
    void twoUsersCanSaveTheSameRecipeName() throws Exception {
        kim.saveMeal(adobo(), List.of());
        jon.saveMeal(adobo(), List.of());
        assertTrue(kim.allMeals().get(0).getRecipe().getSavedRecipeId() != jon.allMeals().get(0).getRecipe().getSavedRecipeId());
    }

    @Test
    void aUserCannotDeleteAnotherUsersMeal() throws Exception {
        long id = kim.saveMeal(adobo(), List.of());
        jon.deleteMeal(id);
        assertEquals(1, kim.allMeals().size());
    }

    @Test
    void aMealForAMissingUserIsRejectedByTheForeignKey() {
        MealPlanDao ghost = new MealPlanDao(conn, 999);
        assertThrows(SQLException.class, () -> ghost.saveMeal(adobo(), List.of()));
    }

    @Test
    void deletingAUserDeletesTheirRecipesAndMeals() throws Exception {
        kim.saveMeal(adobo(), List.of());
        try (Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM users WHERE user_id = " + kim.getUserId());
        }
        assertTrue(kim.allMeals().isEmpty());
        assertFalse(kim.findRecipe("Chicken Adobo").isPresent());
    }

    @Test
    void passwordsAndAnswersAreStoredOnlyAsHashes() throws Exception {
        User found = users.findByUsername("KIM").orElseThrow();
        assertTrue(users.passwordMatches(found.getUserId(), "secret1"));
        assertFalse(users.passwordMatches(found.getUserId(), "Secret1"));
        assertTrue(users.answerMatches(found.getUserId(), "cebu"));
        assertFalse(users.answerMatches(found.getUserId(), "manila"));
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("SELECT password_hash, security_answer_hash FROM users WHERE username = 'kim'")) {
            assertTrue(rs.next());
            assertFalse(rs.getString(1).contains("secret1"));
            assertFalse(rs.getString(2).contains("cebu"));
        }
    }

    @Test
    void theDisplayNameCanChange() throws Exception {
        User found = users.findByUsername("jon").orElseThrow();
        assertTrue(users.updateDisplayName(found.getUserId(), "Jonathan"));
        assertEquals("Jonathan", users.findById(found.getUserId()).orElseThrow().getDisplayName());
        assertFalse(users.updateDisplayName(999, "Nobody"));
    }
}
