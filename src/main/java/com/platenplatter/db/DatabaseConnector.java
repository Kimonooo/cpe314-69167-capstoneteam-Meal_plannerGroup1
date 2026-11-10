package com.platenplatter.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

/**
 * Opens connections to the SQLite file and creates the 5 tables from schema.sql.
 * Every connection switches foreign keys on (SQLite ignores them otherwise) and waits up to
 * 5 seconds for a busy database instead of failing with "database is locked".
 */
public final class DatabaseConnector {
    /** The database file, made in the folder the app is started from. */
    public static final String DEFAULT_URL = "jdbc:sqlite:plate_n_platter.db";

    private DatabaseConnector() { }

    public static Connection open() throws SQLException {
        return open(DEFAULT_URL);
    }

    public static Connection open(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
            st.execute("PRAGMA synchronous = OFF");     // faster imports; fine for class data
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /** Runs schema.sql. Safe every time the app starts, because each table is CREATE ... IF NOT EXISTS. */
    public static void createTables(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(readSchema());   // SQLite runs every statement in the script
        }
    }

    /** The names of the tables in the database, A to Z (used by the tests). */
    public static List<String> tableNames(Connection connection) throws SQLException {
        String sql = "SELECT group_concat(name) FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'";
        try (PreparedStatement ps = connection.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            String names = rs.next() ? rs.getString(1) : null;
            return names == null ? List.of() : Arrays.stream(names.split(",")).sorted().toList();
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream in = DatabaseConnector.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql was not found on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql", e);
        }
    }
}
