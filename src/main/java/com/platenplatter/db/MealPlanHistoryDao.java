package com.platenplatter.db;

import com.platenplatter.util.ImportRow;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The SQLite side of the meal plan. Two tables:
 *   meal_history    - every planned meal (added by hand, or imported from a CSV)
 *   meal_ingredient - the ingredient list of a recipe (used by the shopping list)
 * Responsibility: SQL only. EVERY query that takes a value uses PreparedStatement with bound
 * parameters (no string-concatenated SQL).
 * Methods are synchronized because a CSV import runs on a background thread while the screens
 * may read at the same time over the same Connection.
 */
public class MealPlanHistoryDao implements PlanStore {
    private final Connection conn;

    public MealPlanHistoryDao(Connection conn) {
        this.conn = conn;
    }

    /** Creates the tables (and a fast date index) if they are missing. Call once at app start. */
    public synchronized void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS meal_history ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "plan_date TEXT NOT NULL, "
                    + "recipe_name TEXT NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0))");
            st.execute("CREATE TABLE IF NOT EXISTS meal_ingredient ("
                    + "recipe_name TEXT NOT NULL COLLATE NOCASE, "
                    + "ingredient TEXT NOT NULL, "
                    + "PRIMARY KEY (recipe_name, ingredient))");
            st.execute("CREATE INDEX IF NOT EXISTS idx_meal_history_date ON meal_history (plan_date)");
            // Speeds up bulk imports (thousands of single-row inserts). Fine for course data.
            st.execute("PRAGMA synchronous = OFF");
        }
    }

    // ------------------------------------------------------------------ writing

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

    /** Saves the meal AND its ingredients together: either both are saved or neither. */
    public synchronized void insertWithIngredients(ImportRow row, List<String> ingredients) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            insert(row);
            saveIngredients(row.getRecipeName(), ingredients);
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    @Override
    public synchronized void saveIngredients(String recipeName, List<String> ingredients) throws SQLException {
        if (ingredients == null || ingredients.isEmpty()) {
            return;
        }
        String sql = "INSERT OR IGNORE INTO meal_ingredient (recipe_name, ingredient) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String ingredient : ingredients) {
                if (ingredient != null && !ingredient.isBlank()) {
                    ps.setString(1, recipeName);
                    ps.setString(2, ingredient.trim());
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    @Override
    public synchronized void deleteMeal(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM meal_history WHERE id = ?")) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Wipes the meal table (handy to reset the demo). */
    public synchronized void deleteAll() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM meal_history")) {
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ reading

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

    /** All meals planned on one date (yyyy-MM-dd). */
    public synchronized List<ImportRow> findByDate(String date) throws SQLException {
        List<ImportRow> out = new ArrayList<>();
        for (PlannedMeal m : queryMeals(date, date)) {
            out.add(new ImportRow(m.getDate(), m.getRecipeName(), m.getCategory(), m.getPrepMinutes()));
        }
        return out;
    }

    public synchronized int count() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*) FROM meal_history");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
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

    private List<PlannedMeal> queryMeals(String from, String to) throws SQLException {
        List<PlannedMeal> out = new ArrayList<>();
        String sql = "SELECT id, plan_date, recipe_name, category, prep_minutes FROM meal_history "
                + "WHERE plan_date BETWEEN ? AND ? ORDER BY plan_date, id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, from);
            ps.setString(2, to);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(new PlannedMeal(rs.getLong("id"), rs.getString("plan_date"),
                            rs.getString("recipe_name"), rs.getString("category"), rs.getInt("prep_minutes")));
                }
            }
        }
        return out;
    }

    // ------------------------------------------------------------------ PlanStore (used by the screens)

    @Override
    public void addMeal(ImportRow row, List<String> ingredients) throws SQLException {
        insertWithIngredients(row, ingredients);
    }

    @Override
    public void addImported(ImportRow row) throws SQLException {
        insert(row);
    }

    @Override
    public synchronized List<PlannedMeal> mealsOn(LocalDate date) throws SQLException {
        return queryMeals(date.toString(), date.toString());
    }

    @Override
    public synchronized List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to) throws SQLException {
        return queryMeals(from.toString(), to.toString());
    }

    @Override
    public synchronized Set<LocalDate> plannedDates() throws SQLException {
        Set<LocalDate> out = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT DISTINCT plan_date FROM meal_history");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                try {
                    out.add(LocalDate.parse(rs.getString(1)));
                } catch (DateTimeParseException ignored) {
                    // a badly written date in the table is skipped, never a crash
                }
            }
        }
        return out;
    }

    @Override
    public synchronized List<String> ingredientsFor(String recipeName) throws SQLException {
        List<String> out = new ArrayList<>();
        String sql = "SELECT ingredient FROM meal_ingredient WHERE recipe_name = ? ORDER BY ingredient";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recipeName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        }
        return out;
    }

    @Override
    public List<String> categories() throws SQLException {
        return getPlannedCategories();
    }

    @Override
    public List<Integer> prepMinutes() throws SQLException {
        return getPrepMinutes();
    }
}
