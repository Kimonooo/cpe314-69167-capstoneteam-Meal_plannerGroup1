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
}
