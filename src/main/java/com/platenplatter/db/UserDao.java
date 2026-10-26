package com.platenplatter.db;

import com.platenplatter.model.User;
import java.sql.SQLException;

/**
 * DAY 1 STUB: only the method signatures, so Nesley can code against them.
 * Day 2: fill in the bodies using PreparedStatement with ? placeholders.
 */
public class UserDao {

    /** Saves a new user. Day 2: INSERT INTO users ... VALUES (?, ?, ?, ?, ?, ?). */
    public void insertUser(User user) throws SQLException {
        throw new UnsupportedOperationException("Day 2: not written yet");
    }

    /** Finds a user by username, or returns null. Day 2: SELECT ... WHERE username = ?. */
    public User findByUsername(String username) throws SQLException {
        throw new UnsupportedOperationException("Day 2: not written yet");
    }
}
