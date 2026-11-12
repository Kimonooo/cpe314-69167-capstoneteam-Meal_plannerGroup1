package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import org.junit.jupiter.api.Test;

class DatabaseConnectorTest {

    private static int pragma(Connection c, String name) throws SQLException {
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery("PRAGMA " + name)) {
            assertTrue(rs.next());
            return rs.getInt(1);
        }
    }

    private static int primaryKeyColumns(Connection c, String table) {
        try (Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT count(*) FROM pragma_table_info('" + table + "') WHERE pk > 0")) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void opensAWorkingConnection() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            assertFalse(c.isClosed());
            assertTrue(c.isValid(2));
        }
    }

    @Test
    void setsTheBusyTimeoutAndSwitchesForeignKeysOn() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            assertEquals(5000, pragma(c, "busy_timeout"));
            assertEquals(1, pragma(c, "foreign_keys"));
        }
    }

    @Test
    void schemaSqlCreatesExactlyTheFivePluralTables() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            DatabaseConnector.createTables(c);
            assertEquals(List.of("ingredient_amounts", "ingredients", "planned_meals", "saved_recipes", "users"),
                    DatabaseConnector.tableNames(c));
        }
    }

    @Test
    void creatingTheTablesTwiceIsSafe() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            DatabaseConnector.createTables(c);
            DatabaseConnector.createTables(c);
            assertEquals(5, DatabaseConnector.tableNames(c).size());
        }
    }

    @Test
    void everyTableHasExactlyOnePrimaryKeyColumn() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            DatabaseConnector.createTables(c);
            DatabaseConnector.tableNames(c).forEach(table -> assertEquals(1, primaryKeyColumns(c, table), table));
        }
    }

    @Test
    void aBrokenUrlThrowsAnSqlException() {
        assertThrows(SQLException.class, () -> DatabaseConnector.open("jdbc:notadatabase:oops"));
    }

    @Test
    void anOlderDatabaseFileGetsTheDateAndTimeColumns() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            DatabaseConnector.createTables(c);
            try (Statement st = c.createStatement()) {             // make it look like a file from before this change
                st.executeUpdate("INSERT INTO users (username, display_name, age, gender, password_hash, salt, "
                        + "security_question, security_answer_hash) VALUES ('old', 'Old', 20, 'F', 'h', 's', 'q', 'a')");
                st.executeUpdate("ALTER TABLE users DROP COLUMN created_at");
                st.executeUpdate("ALTER TABLE planned_meals DROP COLUMN updated_at");
            }
            assertFalse(DatabaseConnector.hasColumn(c, "users", "created_at"));
            DatabaseConnector.createTables(c);                     // the app starts again
            assertTrue(DatabaseConnector.hasColumn(c, "users", "created_at"));
            assertTrue(DatabaseConnector.hasColumn(c, "planned_meals", "updated_at"));
            try (Statement st = c.createStatement();
                 ResultSet rs = st.executeQuery("SELECT created_at FROM users WHERE username = 'old'")) {
                assertTrue(rs.next());
                assertEquals(19, rs.getString(1).length());        // the row that was already there is filled in
            }
        }
    }
}
