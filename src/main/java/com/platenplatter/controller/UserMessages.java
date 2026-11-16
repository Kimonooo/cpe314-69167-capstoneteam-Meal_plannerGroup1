package com.platenplatter.controller;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.service.AuthException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Turns any error into a short sentence for the screen. Our own rule messages (a wrong password,
 * a bad prep time) are shown as they are. Database and internet errors get a plain sentence; their
 * technical text is written to the log instead of being shown, so the user never sees an
 * exception message such as "[SQLITE_CONSTRAINT_UNIQUE] ...".
 */
public final class UserMessages {
    public static final String OFFLINE =
            "No internet connection. Check your Wi-Fi, then press Retry.";
    public static final String NOT_RESPONDING =
            "TheMealDB is not responding right now. Try again in a minute.";
    public static final String DATABASE = "Your data could not be read or saved. Please try again.";
    public static final String TAKEN = "That username is already taken. Please choose another.";
    public static final String UNKNOWN = "Something went wrong. Please try again.";

    private static final Logger LOG = Logger.getLogger("PlateNPlatter");

    private UserMessages() { }

    /** The sentence to show for this error. */
    public static String of(Throwable error) {
        if (error instanceof AuthException) {
            return error.getMessage();
        }
        if (error instanceof CancellationException) {
            return "Cancelled. Nothing was changed.";
        }
        LOG.log(Level.WARNING, "Shown to the user as a short message", error);
        if (has(error, SQLException.class)) {
            String text = String.valueOf(rootMessage(error));
            return text.contains("UNIQUE") && text.contains("users.username") ? TAKEN : DATABASE;
        }
        if (error != null && MealDbClient.isOffline(error)) {
            return OFFLINE;
        }
        return has(error, IOException.class) ? NOT_RESPONDING : UNKNOWN;
    }

    private static boolean has(Throwable error, Class<? extends Throwable> type) {
        return Stream.iterate(error, e -> e != null, Throwable::getCause)
                .anyMatch(type::isInstance);
    }

    private static String rootMessage(Throwable error) {
        return Stream.iterate(error, e -> e != null, Throwable::getCause)
                .map(Throwable::getMessage).filter(m -> m != null).reduce((a, b) -> a + " " + b)
                .orElse("");
    }
}
