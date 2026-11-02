package com.platenplatter.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** Opens the SQLite database file and creates the tables from schema.sql. */
public class Database {
    private static final String URL = "jdbc:sqlite:platenplatter.db";

    private Database() { }

    /** Opens a connection to the SQLite file (it is created if it does not exist). */
    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(URL);
        // SQLite ignores foreign keys unless this is switched on for each connection
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA foreign_keys = ON")) {
            ps.execute();
        }
        return conn;
    }

    /** Runs every CREATE TABLE statement in schema.sql (safe to run many times). */
    public static void initialize() throws SQLException {
        String script = readSchema();
        try (Connection conn = getConnection()) {
            for (String chunk : script.split(";")) {
                String sql = removeComments(chunk).trim();
                if (sql.isEmpty()) {
                    continue;
                }
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.execute();
                }
            }
        }
    }

    /** Returns the names of all tables in the database (used to prove the schema exists). */
    public static List<String> listTables() throws SQLException {
        List<String> tables = new ArrayList<>();
        String sql = "SELECT name FROM sqlite_master WHERE type = ? AND name NOT LIKE 'sqlite_%' ORDER BY name";
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "table");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString("name"));
                }
            }
        }
        return tables;
    }

    private static String readSchema() throws SQLException {
        try (InputStream in = Database.class.getResourceAsStream("/schema.sql")) {
            if (in == null) {
                throw new SQLException("schema.sql was not found on the classpath");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new SQLException("Could not read schema.sql", e);
        }
    }

    private static String removeComments(String text) {
        StringBuilder result = new StringBuilder();
        for (String line : text.split("\n")) {
            if (!line.trim().startsWith("--")) {
                result.append(line).append("\n");
            }
        }
        return result.toString();
    }
}
