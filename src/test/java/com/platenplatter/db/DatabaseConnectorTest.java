package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.*;

import com.platenplatter.util.ImportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.junit.jupiter.api.Test;

class DatabaseConnectorTest {

    @Test
    void opensAWorkingConnection() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            assertFalse(c.isClosed());
            assertTrue(c.isValid(2));
        }
    }

    @Test
    void setsTheBusyTimeoutToFiveSeconds() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:");
             PreparedStatement ps = c.prepareStatement("PRAGMA busy_timeout");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(5000, rs.getInt(1));
        }
    }

    @Test
    void switchesForeignKeysOn() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:");
             PreparedStatement ps = c.prepareStatement("PRAGMA foreign_keys");
             ResultSet rs = ps.executeQuery()) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }

    @Test
    void theConnectionWorksWithTheMealHistoryDao() throws SQLException {
        try (Connection c = DatabaseConnector.open("jdbc:sqlite::memory:")) {
            MealPlanHistoryDao dao = new MealPlanHistoryDao(c);
            dao.initSchema();
            dao.insert(new ImportRow("2026-11-03", "Tapsilog", "Breakfast", 20));
            assertEquals(1, dao.count());
        }
    }

    @Test
    void aBrokenUrlThrowsAnSqlException() {
        assertThrows(SQLException.class, () -> DatabaseConnector.open("jdbc:notadatabase:oops"));
    }
}
