package com.platenplatter.db;

import com.platenplatter.util.ImportRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Data access for the meal_history table (planned meals + imported history).
 * Responsibility: SQL only. EVERY query that takes a value uses PreparedStatement with
 * bound parameters (no string-concatenated SQL).
 * Methods are synchronized because the import runs on a background thread while the UI
 * thread may read at the same time over the same Connection.
 */
public class MealPlanHistoryDao {
    private final Connection conn;

    public MealPlanHistoryDao(Connection conn) {
        this.conn = conn;
    }

    /** Creates the table if it is missing. Call once at app start. */
    public synchronized void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS meal_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "plan_date TEXT NOT NULL, "
                    + "recipe_name TEXT NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0))");
            // Speeds up bulk imports (thousands of single-row inserts). Fine for course data.
            st.execute("PRAGMA synchronous = OFF");
        }
    }

    public synchronized void insert(ImportRow row) throws SQLException {
        String sql = "INSERT INTO meal_history (plan_date, recipe_name, category, prep_minutes) "
                + "VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, row.getDate());
            ps.setString(2, row.getRecipeName());
            ps.setString(3, row.getCategory());
            ps.setInt(4, row.getPrepMinutes());
            ps.executeUpdate();
        }
    }

    /** Category of every planned meal - the input of the data-mining step. */
    public synchronized List<String> getPlannedCategories() throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT category FROM meal_history ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(rs.getString("category"));
            }
        }
        return out;
    }

    public synchronized List<Integer> getPrepMinutes() throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT prep_minutes FROM meal_history ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(rs.getInt("prep_minutes"));
            }
        }
        return out;
    }

    /** All meals planned on one date (yyyy-MM-dd) - the Calendar tab can use this. */
    public synchronized List<ImportRow> findByDate(String date) throws SQLException {
        List<ImportRow> out = new ArrayList<>();
        String sql = "SELECT plan_date, recipe_name, category, prep_minutes FROM meal_history "
                + "WHERE plan_date = ? ORDER BY id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, date);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new ImportRow(rs.getString("plan_date"), rs.getString("recipe_name"),
                            rs.getString("category"), rs.getInt("prep_minutes")));
                }
            }
        }
        return out;
    }

    public synchronized int count() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM meal_history");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Wipes the table (handy to reset the demo). */
    public synchronized void deleteAll() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM meal_history")) {
            ps.executeUpdate();
        }
    }

    public synchronized int countByCategory(String category) throws SQLException {
String sql = "SELECT COUNT(*) FROM meal_history WHERE category = ?";
try (PreparedStatement ps = conn.prepareStatement(sql)) {
ps.setString(1, category);
try (ResultSet rs = ps.executeQuery()) {
return rs.next() ? rs.getInt(1) : 0;
         }
      }
   }
}
