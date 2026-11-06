package com.platenplatter.db;

import com.platenplatter.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Talks to the users table. Every query uses PreparedStatement with ? placeholders,
 * so the values are filled in safely and no SQL is built by gluing strings together.
 *
 * Two ways to use it:
 *   new UserDao()            opens a short connection to platenplatter.db for every call (the app)
 *   new UserDao(connection)  uses one connection you give it and never closes it (tests, in memory)
 */
public class UserDao {
    private static final String COLUMNS = "id, name, username, age, gender, password_hash, salt";

    private final Connection shared;

    public UserDao() {
        this(null);
    }

    public UserDao(Connection shared) {
        this.shared = shared;
    }

    private Connection open() throws SQLException {
        return shared != null ? shared : Database.getConnection();
    }

    private void release(Connection conn) throws SQLException {
        if (conn != shared) {
            conn.close();
        }
    }

    /** Saves a new user (the id is made by the database). */
    public synchronized void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, username, age, gender, password_hash, salt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = open();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getUsername());
            ps.setInt(3, user.getAge());
            ps.setString(4, user.getGender());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getSalt());
            ps.executeUpdate();
        } finally {
            release(conn);
        }
    }

    /**
     * Finds a user by username, or returns null if there is no such user.
     * Upper and lower case count as the same ("Jon" finds "jon"), so two people can never
     * have usernames that differ only in case.
     */
    public synchronized User findByUsername(String username) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE username = ? COLLATE NOCASE";
        Connection conn = open();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            return readOne(ps);
        } finally {
            release(conn);
        }
    }

    /** Finds a user by id, or returns null. */
    public synchronized User findById(int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM users WHERE id = ?";
        Connection conn = open();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return readOne(ps);
        } finally {
            release(conn);
        }
    }

    /** Changes a user's display name. Returns true if the user exists. */
    public synchronized boolean updateName(int id, String newName) throws SQLException {
        String sql = "UPDATE users SET name = ? WHERE id = ?";
        Connection conn = open();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newName);
            ps.setInt(2, id);
            return ps.executeUpdate() == 1;
        } finally {
            release(conn);
        }
    }

    private static User readOne(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("username"),
                        rs.getInt("age"),
                        rs.getString("gender"),
                        rs.getString("password_hash"),
                        rs.getString("salt"));
            }
            return null;
        }
    }
}
