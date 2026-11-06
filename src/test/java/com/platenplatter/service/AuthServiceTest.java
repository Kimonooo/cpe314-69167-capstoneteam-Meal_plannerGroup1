package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.MealPlanHistoryDao;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;

import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Sign up, log in and rename, on a throw-away database in memory (never touches platenplatter.db). */
class AuthServiceTest {
    private Connection conn;
    private UserDao users;
    private AuthService auth;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        new MealPlanHistoryDao(conn).initSchema(); // creates the users table too
        users = new UserDao(conn);
        auth = new AuthService(users);
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private User registerJon() throws Exception {
        return auth.register("Jon", "jon", "21", "M", "secret123", "secret123");
    }

    @Test
    void aNewAccountCanLogIn() throws Exception {
        User saved = registerJon();
        assertTrue(saved.getId() > 0);
        User loggedIn = auth.login("jon", "secret123");
        assertEquals(saved.getId(), loggedIn.getId());
        assertEquals("Jon", loggedIn.getName());
    }

    @Test
    void thePasswordIsNeverSavedAsPlainText() throws Exception {
        User saved = registerJon();
        assertNotEquals("secret123", saved.getPasswordHash());
        assertFalse(saved.getSalt().isBlank());
    }

    @Test
    void aWrongPasswordOrUnknownUserGivesTheSameMessage() throws Exception {
        registerJon();
        AuthException wrongPassword = assertThrows(AuthException.class, () -> auth.login("jon", "wrong-pass"));
        AuthException unknownUser = assertThrows(AuthException.class, () -> auth.login("nobody", "secret123"));
        assertEquals("Incorrect username or password.", wrongPassword.getMessage());
        assertEquals(wrongPassword.getMessage(), unknownUser.getMessage());
    }

    @Test
    void emptyLoginFieldsAreRejected() {
        assertThrows(AuthException.class, () -> auth.login("", "secret123"));
        assertThrows(AuthException.class, () -> auth.login("jon", ""));
        assertThrows(AuthException.class, () -> auth.login(null, null));
    }

    @Test
    void usernamesAreTheSameInUpperAndLowerCase() throws Exception {
        registerJon();
        assertEquals("jon", auth.login("JON", "secret123").getUsername());
        AuthException taken = assertThrows(AuthException.class,
                () -> auth.register("Other Jon", "Jon", "30", "M", "secret123", "secret123"));
        assertTrue(taken.getMessage().contains("taken"));
    }

    @Test
    void everySignUpRuleIsChecked() {
        assertThrows(AuthException.class, () -> auth.register("", "jon", "21", "M", "secret123", "secret123"));
        assertThrows(AuthException.class, () -> auth.register("Jon", "jo", "21", "M", "secret123", "secret123"));      // too short
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon phil", "21", "M", "secret123", "secret123")); // space
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "abc", "M", "secret123", "secret123"));
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "0", "M", "secret123", "secret123"));
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "121", "M", "secret123", "secret123"));
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "21", null, "secret123", "secret123"));
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "21", "M", "12345", "12345"));           // 5 characters
        assertThrows(AuthException.class, () -> auth.register("Jon", "jon", "21", "M", "secret123", "secret124"));
    }

    @Test
    void theDisplayNameCanBeChangedAndIsSaved() throws Exception {
        User jon = registerJon();
        User renamed = auth.rename(jon, "  Jon Phil  ");
        assertEquals("Jon Phil", renamed.getName());
        assertEquals("jon", renamed.getUsername()); // the username never changes
        assertEquals("Jon Phil", users.findById(jon.getId()).getName());
        assertEquals("Jon Phil", auth.login("jon", "secret123").getName());
    }

    @Test
    void aBlankOrTooLongNameIsRejectedAndNothingChanges() throws Exception {
        User jon = registerJon();
        assertThrows(AuthException.class, () -> auth.rename(jon, "   "));
        assertThrows(AuthException.class, () -> auth.rename(jon, "x".repeat(AuthService.MAX_NAME_LENGTH + 1)));
        assertEquals("Jon", users.findById(jon.getId()).getName());
    }

    @Test
    void renamingAUserWhoIsGoneIsRejected() {
        User ghost = new User(999, "Ghost", "ghost", 20, "F", "x", "y");
        assertThrows(AuthException.class, () -> auth.rename(ghost, "New Name"));
    }
}
