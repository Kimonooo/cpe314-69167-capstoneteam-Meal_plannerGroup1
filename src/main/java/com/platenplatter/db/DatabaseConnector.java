package com.platenplatter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * The one place that opens the app's SQLite database for the Milestone 2 tabs.
 * Responsibility: open a connection and set it up:
 *   - foreign_keys = ON: SQLite ignores foreign keys unless this is switched on for each connection,
 *     so a meal can never point at a user who does not exist;
 *   - busy_timeout = 5000: WAIT (up to 5 seconds) instead of failing when another connection
 *     is writing at the same moment.
 *
 * It must point at the SAME file the rest of the app uses (Database.java uses platenplatter.db too).
 */
public final class DatabaseConnector {
    /** The database file in the project folder (the file called platenplatter.db). */
    public static final String DEFAULT_URL = "jdbc:sqlite:platenplatter.db";

    private DatabaseConnector() { }

    public static Connection open() throws SQLException {
        return open(DEFAULT_URL);
    }

    public static Connection open(String url) throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (Statement st = connection.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON");
            st.execute("PRAGMA busy_timeout = 5000");
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }
}
