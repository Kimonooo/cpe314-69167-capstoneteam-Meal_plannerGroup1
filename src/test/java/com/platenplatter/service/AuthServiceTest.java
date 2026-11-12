package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.db.DatabaseConnector;
import com.platenplatter.db.UserDao;
import com.platenplatter.model.User;
import java.sql.Connection;
import java.sql.SQLException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceTest {
    private static final String PET = AuthService.SECURITY_QUESTIONS.get(0);

    private Connection conn;
    private AuthService auth;

    @BeforeEach
    void setUp() throws SQLException {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        auth = new AuthService(new UserDao(conn));
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private User registerAna() throws Exception {
        return auth.register("Ana", "ana", "20", "F", "secret1", "secret1", PET, "Bantay");
    }

    @Test
    void signUpThenLogInWorks() throws Exception {
        User ana = registerAna();
        assertTrue(ana.getUserId() > 0);
        assertEquals("Ana", auth.login("ana", "secret1").getDisplayName());
        assertEquals(PET, ana.getSecurityQuestion());
    }

    @Test
    void usernamesIgnoreUpperAndLowerCase() throws Exception {
        registerAna();
        assertEquals("ana", auth.login("ANA", "secret1").getUsername());
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register("Other", "Ana", "30", "M", "secret2", "secret2", PET, "x"));
        assertTrue(e.getMessage().contains("taken"));
    }

    @Test
    void aWrongPasswordOrUnknownUserGivesTheSameMessage() throws Exception {
        registerAna();
        AuthException wrong = assertThrows(AuthException.class, () -> auth.login("ana", "secret2"));
        AuthException unknown = assertThrows(AuthException.class, () -> auth.login("nobody", "secret1"));
        assertEquals(wrong.getMessage(), unknown.getMessage());
    }

    @Test
    void signUpRulesAreChecked() {
        assertThrows(AuthException.class, () -> auth.register("", "ana", "20", "F", "secret1", "secret1", PET, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "a b", "20", "F", "secret1", "secret1", PET, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "abc", "F", "secret1", "secret1", PET, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "121", "F", "secret1", "secret1", PET, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "20", "F", "short", "short", PET, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "20", "F", "secret1", "secret2", PET, "x"));
    }

    @Test
    void aSecurityQuestionAndAnswerAreRequired() {
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "20", "F", "secret1", "secret1", null, "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "20", "F", "secret1", "secret1", "My own question?", "x"));
        assertThrows(AuthException.class, () -> auth.register("Ana", "ana", "20", "F", "secret1", "secret1", PET, "   "));
    }

    @Test
    void sixSpacesAreNotAPassword() {
        AuthException e = assertThrows(AuthException.class,
                () -> auth.register("Ana", "ana", "20", "F", "      ", "      ", PET, "x"));
        assertTrue(e.getMessage().contains("not only spaces"));
    }

    @Test
    void renameChangesTheDisplayNameButNotTheUsername() throws Exception {
        User ana = registerAna();
        User renamed = auth.rename(ana, "  Ana Marie ");
        assertEquals("Ana Marie", renamed.getDisplayName());
        assertEquals("Ana Marie", auth.login("ana", "secret1").getDisplayName());
        assertThrows(AuthException.class, () -> auth.rename(ana, " "));
        assertThrows(AuthException.class, () -> auth.rename(ana, "x".repeat(41)));
    }

    @Test
    void forgotPasswordShowsTheChosenQuestion() throws Exception {
        registerAna();
        assertEquals(PET, auth.securityQuestionFor(" ANA "));
        assertThrows(AuthException.class, () -> auth.securityQuestionFor("nobody"));
    }

    @Test
    void theRightAnswerResetsThePasswordIgnoringCaseAndSpaces() throws Exception {
        registerAna();
        auth.resetPassword("ana", " BANTAY ", "newpass1", "newpass1");
        assertEquals("Ana", auth.login("ana", "newpass1").getDisplayName());
        assertThrows(AuthException.class, () -> auth.login("ana", "secret1"));     // the old one stops working
    }

    @Test
    void aWrongAnswerIsRefusedAndTheOldPasswordStillWorks() throws Exception {
        registerAna();
        AuthException e = assertThrows(AuthException.class, () -> auth.resetPassword("ana", "Muning", "newpass1", "newpass1"));
        assertEquals("That answer does not match.", e.getMessage());
        assertEquals("Ana", auth.login("ana", "secret1").getDisplayName());
    }

    @Test
    void theNewPasswordFollowsTheSameRules() throws Exception {
        registerAna();
        assertThrows(AuthException.class, () -> auth.resetPassword("ana", "bantay", "      ", "      "));
        assertThrows(AuthException.class, () -> auth.resetPassword("ana", "bantay", "newpass1", "newpass2"));
        assertThrows(AuthException.class, () -> auth.resetPassword("nobody", "bantay", "newpass1", "newpass1"));
    }

    @Test
    void theAnswerIsNormalizedBeforeHashing() {
        assertEquals("bantay", AuthService.normalize("  BanTay "));
        assertEquals("", AuthService.normalize(null));
    }

    @Test
    void theRightPasswordDeletesTheAccountAndTheUsernameIsFreeAgain() throws Exception {
        User ana = registerAna();
        auth.deleteAccount(ana, "secret1");
        assertThrows(AuthException.class, () -> auth.login("ana", "secret1"));
        assertTrue(registerAna().getUserId() > 0);                  // the same username can sign up again
    }

    @Test
    void aWrongOrEmptyPasswordDeletesNothing() throws Exception {
        User ana = registerAna();
        AuthException wrong = assertThrows(AuthException.class, () -> auth.deleteAccount(ana, "secret2"));
        assertTrue(wrong.getMessage().contains("Nothing was deleted"));
        assertThrows(AuthException.class, () -> auth.deleteAccount(ana, ""));
        assertEquals("Ana", auth.login("ana", "secret1").getDisplayName());
    }
}
