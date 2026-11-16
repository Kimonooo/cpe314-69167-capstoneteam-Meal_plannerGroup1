package com.platenplatter.db;

import com.platenplatter.model.User;
import com.platenplatter.util.PasswordHasher;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * All SQL for the users table. Every query uses PreparedStatement.
 * Passwords and security answers are never saved: only a salted PBKDF2 hash of each.
 * One random salt per user is used for both hashes.
 * Every method locks the shared Connection (synchronized (conn)), the same lock MealPlanDao uses.
 */
public class UserDao {
    private static final String USER_COLUMNS =
            "user_id, username, display_name, age, gender, security_question";

    private final Connection conn;

    public UserDao(Connection conn) {
        this.conn = conn;
    }

    /**
     * Saves a new account and returns it with its new user_id. The answer must already be
     * normalized.
     */
    public User insertUser(User user, String password, String securityAnswer) throws SQLException {
        synchronized (conn) {
            String sql = "INSERT INTO users (username, display_name, age, gender, password_hash, "
                    + "salt, security_question, security_answer_hash, created_at, updated_at) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
            String salt = PasswordHasher.generateSalt();
            try (PreparedStatement ps = conn.prepareStatement(sql,
                    Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getDisplayName());
                ps.setInt(3, user.getAge());
                ps.setString(4, user.getGender());
                // the real password is never saved
                ps.setString(5, PasswordHasher.hash(password, salt));
                ps.setString(6, salt);
                ps.setString(7, user.getSecurityQuestion());
                ps.setString(8, PasswordHasher.hash(securityAnswer, salt)); // nor the real answer
                ps.executeUpdate();
                return new User(newId(ps), user.getUsername(), user.getDisplayName(), user.getAge(),
                        user.getGender(), user.getSecurityQuestion());
            }
        }
    }

    /** The account with this username, ignoring upper/lower case. */
    public Optional<User> findByUsername(String username) throws SQLException {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT " + USER_COLUMNS + " FROM users WHERE username = ?")) {
                ps.setString(1, username);
                return readOne(ps);
            }
        }
    }

    public Optional<User> findById(long userId) throws SQLException {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement("SELECT " + USER_COLUMNS
                    + " FROM users WHERE user_id = ?")) {
                ps.setLong(1, userId);
                return readOne(ps);
            }
        }
    }

    /** True when the password hashes to the saved password_hash. */
    public boolean passwordMatches(long userId, String password) throws SQLException {
        synchronized (conn) {
            return hashMatches(userId, "password_hash", password);
        }
    }

    /**
     * True when the (already trimmed, lower-case) answer hashes to the saved security_answer_hash.
     */
    public boolean answerMatches(long userId, String normalizedAnswer) throws SQLException {
        synchronized (conn) {
            return hashMatches(userId, "security_answer_hash", normalizedAnswer);
        }
    }

    /** Saves the PBKDF2 hash of a new password, with the user's own salt. */
    public void updatePassword(long userId, String newPassword) throws SQLException {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE user_id = ?")) {
                ps.setString(1, PasswordHasher.hash(newPassword, saltOf(userId)));
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        }
    }

    public boolean updateDisplayName(long userId, String displayName) throws SQLException {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET display_name = ?, updated_at = CURRENT_TIMESTAMP "
                    + "WHERE user_id = ?")) {
                ps.setString(1, displayName);
                ps.setLong(2, userId);
                return ps.executeUpdate() == 1;
            }
        }
    }

    /**
     * Deletes the account. ON DELETE CASCADE in schema.sql removes the user's saved recipes
     * (with their ingredient amounts) and planned meals in the same statement. The shared
     * ingredients list has no user data and is kept. True when an account was deleted.
     */
    public boolean deleteUser(long userId) throws SQLException {
        synchronized (conn) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM users WHERE user_id = ?")) {
                ps.setLong(1, userId);
                return ps.executeUpdate() == 1;
            }
        }
    }

    private boolean hashMatches(long userId, String column, String secret) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                // column is one of our own two names
                "SELECT salt, " + column + " FROM users WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && secret != null && PasswordHasher.verify(secret, rs.getString(1),
                        rs.getString(2));
            }
        }
    }

    private String saltOf(long userId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT salt FROM users WHERE user_id = ?")) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No user with id " + userId);
                }
                return rs.getString(1);
            }
        }
    }

    private static Optional<User> readOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            return rs.next()
                    ? Optional.of(new User(rs.getLong("user_id"), rs.getString("username"),
                            rs.getString("display_name"),
                            rs.getInt("age"), rs.getString("gender"),
                                    rs.getString("security_question")))
                    : Optional.empty();
        }
    }

    private static long newId(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            return keys.next() ? keys.getLong(1) : 0;
        }
    }
}
