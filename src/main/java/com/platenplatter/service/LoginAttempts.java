package com.platenplatter.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Counts wrong passwords and wrong security answers per username. After MAX_TRIES wrong tries in
 * a row, that username is locked for LOCK_TIME: nobody can log in or reset its password until
 * the time is up, so a password or an answer cannot be guessed by trying again and again.
 * A right password clears the count. The clock is passed in, so the tests do not have to wait.
 */
public class LoginAttempts {
    public static final int MAX_TRIES = 5;
    public static final Duration LOCK_TIME = Duration.ofMinutes(5);

    private final Clock clock;
    private final Map<String, Integer> wrongTries = new HashMap<>();
    private final Map<String, Instant> lockedUntil = new HashMap<>();

    public LoginAttempts() {
        this(Clock.systemUTC());
    }

    public LoginAttempts(Clock clock) {
        this.clock = clock;
    }

    /** True while the username is locked. A lock that ran out is removed. */
    public synchronized boolean isLocked(String username) {
        Instant until = lockedUntil.get(key(username));
        if (until != null && !clock.instant().isBefore(until)) {
            lockedUntil.remove(key(username));
            return false;
        }
        return until != null;
    }

    /** Whole minutes left on the lock, at least 1 while it is locked. */
    public synchronized long minutesLeft(String username) {
        Instant until = lockedUntil.get(key(username));
        if (until == null) {
            return 0;
        }
        long seconds = Duration.between(clock.instant(), until).getSeconds();
        return Math.max(1, (seconds + 59) / 60);
    }

    /** One more wrong try; the fifth in a row locks the username. */
    public synchronized void failed(String username) {
        int tries = wrongTries.merge(key(username), 1, Integer::sum);
        if (tries >= MAX_TRIES) {
            wrongTries.remove(key(username));
            lockedUntil.put(key(username), clock.instant().plus(LOCK_TIME));
        }
    }

    /** A right password: the count starts again from zero. */
    public synchronized void succeeded(String username) {
        wrongTries.remove(key(username));
        lockedUntil.remove(key(username));
    }

    /** Wrong tries so far (for the tests and the warning text). */
    public synchronized int wrongTries(String username) {
        return wrongTries.getOrDefault(key(username), 0);
    }

    private static String key(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
