package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.UserDao;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Five wrong passwords or answers lock a username for five minutes; unknown names give no hint. */
class AuthLockoutTest {
    private static final String PET = AuthService.SECURITY_QUESTIONS.get(0);

    private final MovableClock clock = new MovableClock();
    private Connection conn;
    private AuthService auth;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        auth = new AuthService(new UserDao(conn), new LoginAttempts(clock));
        auth.register("Ana", "ana", "20", "Female", "secret1", "secret1", PET, "Bantay");
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private void wrongPasswords(int times) {
        for (int i = 0; i < times; i++) {
            assertThrows(AuthException.class, () -> auth.login("ana", "wrong-1"));
        }
    }

    @Test
    void theFifthWrongPasswordLocksTheAccountEvenForTheRightPassword() {
        wrongPasswords(4);
        AuthException fifth = assertThrows(AuthException.class, () -> auth.login("ana", "wrong-1"));
        assertTrue(fifth.getMessage().startsWith("Too many wrong tries"));
        AuthException locked = assertThrows(AuthException.class, () -> auth.login("ana",
                "secret1"));
        assertTrue(locked.getMessage().contains("Wait 5 minutes"));
    }

    @Test
    void afterFiveMinutesTheRightPasswordWorksAgain() throws Exception {
        wrongPasswords(5);
        clock.forward(Duration.ofMinutes(5));
        assertEquals("Ana", auth.login("ana", "secret1").getDisplayName());
    }

    @Test
    void wrongAnswersLockThePasswordResetToo() {
        for (int i = 0; i < LoginAttempts.MAX_TRIES; i++) {
            assertThrows(AuthException.class, () -> auth.resetPassword("ana", "muning", "newpass1",
                    "newpass1"));
        }
        AuthException e = assertThrows(AuthException.class,
                // right, but too late
                () -> auth.resetPassword("ana", "bantay", "newpass1", "newpass1"));
        assertTrue(e.getMessage().startsWith("Too many wrong tries"));
    }

    @Test
    void anUnknownNameAndAWrongAnswerGiveTheSameMessage() {
        AuthException unknown = assertThrows(AuthException.class,
                () -> auth.resetPassword("nobody", "bantay", "newpass1", "newpass1"));
        AuthException wrong = assertThrows(AuthException.class,
                () -> auth.resetPassword("ana", "muning", "newpass1", "newpass1"));
        assertEquals(AuthService.ANSWER_DOES_NOT_MATCH, unknown.getMessage());
        assertEquals(unknown.getMessage(), wrong.getMessage());
    }

    @Test
    void anUnknownNameAlwaysGetsTheSameQuestion() throws Exception {
        String first = auth.securityQuestionFor("nobody");
        assertEquals(first, auth.securityQuestionFor("NOBODY"));
        // blank: asked to type it
        assertThrows(AuthException.class, () -> auth.securityQuestionFor("   "));
    }
}
