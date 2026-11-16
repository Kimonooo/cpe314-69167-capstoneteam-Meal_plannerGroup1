package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class LoginAttemptsTest {
    private final MovableClock clock = new MovableClock();
    private final LoginAttempts attempts = new LoginAttempts(clock);

    private void fail(String name, int times) {
        for (int i = 0; i < times; i++) {
            attempts.failed(name);
        }
    }

    @Test
    void fourWrongTriesDoNotLockButTheFifthDoes() {
        fail("ana", 4);
        assertFalse(attempts.isLocked("ana"));
        attempts.failed("ana");
        assertTrue(attempts.isLocked("ana"));
        assertEquals(5, attempts.minutesLeft("ana"));
    }

    @Test
    void theLockEndsAfterFiveMinutes() {
        fail("ana", 5);
        clock.forward(Duration.ofMinutes(4).plusSeconds(59));
        assertTrue(attempts.isLocked("ana"));
        clock.forward(Duration.ofSeconds(1));                  // edge: exactly five minutes later
        assertFalse(attempts.isLocked("ana"));
    }

    @Test
    void aRightPasswordStartsTheCountAgain() {
        fail("ana", 4);
        attempts.succeeded("ana");
        assertEquals(0, attempts.wrongTries("ana"));
        attempts.failed("ana");
        assertFalse(attempts.isLocked("ana"));
    }

    @Test
    void namesAreCountedIgnoringCaseAndSpaces() {
        fail("Ana", 3);
        fail(" ANA ", 2);
        assertTrue(attempts.isLocked("ana"));
        assertFalse(attempts.isLocked("ben"));               // another name is not touched
    }
}
