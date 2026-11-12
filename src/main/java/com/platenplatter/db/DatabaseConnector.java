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
            st.execute("PRAGMA journal_mode = WAL");      // writes go to a log first, so a crash cannot corrupt the file
            st.execute("PRAGMA synchronous = NORMAL");    // safe together with WAL, and much faster than FULL

        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    /**
     * Runs schema.sql. Safe every time the app starts, because each table is CREATE ... IF NOT EXISTS.
     * A database file made before created_at and updated_at existed gets those columns added,
     * so nobody has to delete their plate_n_platter.db.
     */
    public static void createTables(Connection connection) throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.executeUpdate(readSchema());   // SQLite runs every statement in the script
        }
        addTimestampIfMissing(connection, "users", "created_at");
        addTimestampIfMissing(connection, "users", "updated_at");
        addTimestampIfMissing(connection, "planned_meals", "created_at");
        addTimestampIfMissing(connection, "planned_meals", "updated_at");
    }

    /** True when the table has a column with this name. */
    public static boolean hasColumn(Connection connection, String table, String column) throws SQLException {
        String sql = "SELECT count(*) FROM pragma_table_info(?) WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    /**
     * Adds a date-and-time column to a table of an older database file and fills the rows already there.
     * SQLite cannot add a column whose default is CURRENT_TIMESTAMP, so the column is added empty
     * and then filled. The table and column names are our own constants, never user text.
     */
    private static void addTimestampIfMissing(Connection connection, String table, String column) throws SQLException {
        if (hasColumn(connection, table, column)) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " TEXT NOT NULL DEFAULT ''");
            st.executeUpdate("UPDATE " + table + " SET " + column + " = CURRENT_TIMESTAMP WHERE " + column + " = ''");
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
