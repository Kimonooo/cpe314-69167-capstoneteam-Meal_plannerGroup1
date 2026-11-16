package com.platenplatter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.api.RateLimitException;
import com.platenplatter.service.AuthException;
import com.platenplatter.service.MealImporter;
import java.io.IOException;
import java.net.ConnectException;
import java.sql.SQLException;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

/** The sentences the pages show: never a raw exception text. */
class ImportAndMessagesTest {

    @Test
    void theImportSummarySaysWhatHappenedInPlainWords() {
        assertEquals("Done! 2 meals imported. 4 bad rows skipped.",
                ImportController.summary(new MealImporter.Result(2, 0, 4, 1)));
        assertEquals("Done! 0 meals imported. 1 meal was already in your plan.",
                ImportController.summary(new MealImporter.Result(0, 1, 0, 0)));
        assertEquals("Done! 1,250 meals imported.",
                ImportController.summary(new MealImporter.Result(1250, 0, 0, 0)));
    }

    @Test
    void aFileProblemIsNotCalledAnInternetProblem() {
        assertTrue(ImportController.failure(new IOException("C:\\\\x.csv (Access is denied)"))
                .startsWith("The file could not be read"));
        assertTrue(ImportController.failure(new SQLException("[SQLITE_BUSY]"))
                .contains("nothing was added"));
    }

    @Test
    void aDatabaseErrorIsNeverShownRaw() {
        String text = UserMessages.of(new SQLException("[SQLITE_CONSTRAINT_UNIQUE] UNIQUE "
                + "constraint failed"));
        assertFalse(text.contains("SQLITE"));
        assertEquals(UserMessages.DATABASE, text);
        assertEquals(UserMessages.TAKEN, UserMessages.of(new SQLException(
                "[SQLITE_CONSTRAINT_UNIQUE] A UNIQUE constraint failed (UNIQUE constraint failed: "
                        + "users.username)")));
    }

    @Test
    void internetProblemsGetTheirOwnSentence() {
        assertEquals(UserMessages.OFFLINE, UserMessages.of(new ConnectException("refused")));
        assertEquals(UserMessages.TOO_MANY_REQUESTS, UserMessages.of(new RateLimitException()));
        assertEquals(UserMessages.NOT_RESPONDING, UserMessages.of(new IOException("HTTP 500")));
    }

    @Test
    void ourOwnRuleMessagesAreShownAsTheyAre() {
        assertEquals("Passwords do not match.",
                UserMessages.of(new AuthException("Passwords do not match.")));
        assertEquals("Prep time must be a whole number from 0 to 1,440 minutes.",
                UserMessages.of(new InvalidInputException("Prep time must be a whole number from "
                        + "0 to 1,440 minutes.")));
        assertEquals("Cancelled. Nothing was changed.",
                UserMessages.of(new CancellationException()));
    }
}
