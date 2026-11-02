package com.platenplatter.db;

import com.platenplatter.model.User;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Talks to the users table. Every query uses PreparedStatement with ? placeholders,
 * so the values are filled in safely and no SQL is built by gluing strings together.
 */
public class UserDao {

    /** Saves a new user (the id is made by the database). */
    public void insertUser(User user) throws SQLException {
        String sql = "INSERT INTO users (name, username, age, gender, password_hash, salt) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getUsername());
            ps.setInt(3, user.getAge());
            ps.setString(4, user.getGender());
            ps.setString(5, user.getPasswordHash());
            ps.setString(6, user.getSalt());
            ps.executeUpdate();
        }
    }

    /** Finds a user by username, or returns null if there is no such user. */
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT id, name, username, age, gender, password_hash, salt "
                + "FROM users WHERE username = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
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
}
