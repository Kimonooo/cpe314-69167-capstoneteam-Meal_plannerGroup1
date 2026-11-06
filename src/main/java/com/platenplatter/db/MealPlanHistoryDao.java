package com.platenplatter.db;

import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.MealSlot;
import com.platenplatter.util.HealthClassifier;
import com.platenplatter.util.ImportRow;
import com.platenplatter.util.PlanStore;
import com.platenplatter.util.PlannedMeal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The SQLite side of the meal plan. Three tables:
 *   users           - the accounts (the same table UserDao reads and writes)
 *   meal_history    - every planned meal: whose it is, the day, the meal slot (Breakfast, Lunch,
 *                     Dinner, Snack), the recipe, its prep minutes, calories and healthy level
 *   meal_ingredient - the ingredient list of a recipe (used by the shopping list)
 *
 * One DAO belongs to ONE logged-in user: every query only sees that user's meals, and every new
 * meal is saved with that user's id. A DAO made without a user id (used by the tests and when the
 * tables are created at start-up) sees every row.
 *
 * Responsibility: SQL only. EVERY query that takes a value uses PreparedStatement with bound
 * parameters (no string-concatenated SQL).
 * Methods are synchronized because a CSV import runs on a background thread while the screens
 * may read at the same time over the same Connection.
 */
public class MealPlanHistoryDao implements PlanStore {

    // ------------------------------------------------------------------ the tables

    static final String CREATE_USERS = "CREATE TABLE IF NOT EXISTS users ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "name TEXT NOT NULL, "
            + "username TEXT NOT NULL UNIQUE, "
            + "age INTEGER, "
            + "gender TEXT, "
            + "password_hash TEXT NOT NULL, "
            + "salt TEXT NOT NULL)";

    static final String CREATE_MEAL_HISTORY = "CREATE TABLE IF NOT EXISTS meal_history ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
            + "user_id INTEGER REFERENCES users(id), "
            + "plan_date TEXT NOT NULL, "
            + "meal_slot TEXT NOT NULL DEFAULT 'DINNER' CHECK (meal_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')), "
            + "recipe_name TEXT NOT NULL, "
            + "category TEXT NOT NULL, "
            + "prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0), "
            + "calories INTEGER NOT NULL DEFAULT 0 CHECK (calories >= 0), "
            + "healthy_level TEXT NOT NULL DEFAULT 'MODERATE' CHECK (healthy_level IN ('HEALTHY', 'MODERATE', 'INDULGENT')))";

    static final String CREATE_MEAL_INGREDIENT = "CREATE TABLE IF NOT EXISTS meal_ingredient ("
            + "recipe_name TEXT NOT NULL COLLATE NOCASE, "
            + "ingredient TEXT NOT NULL, "
            + "PRIMARY KEY (recipe_name, ingredient))";

    // A database file made by the Milestone 2 build has the old meal_history (5 columns).
    // CREATE TABLE IF NOT EXISTS never changes a table that is already there, so the new
    // columns are added one by one with ALTER TABLE.
    static final String ADD_USER_ID = "ALTER TABLE meal_history ADD COLUMN user_id INTEGER REFERENCES users(id)";
    static final String ADD_MEAL_SLOT = "ALTER TABLE meal_history ADD COLUMN meal_slot TEXT NOT NULL DEFAULT 'DINNER' "
            + "CHECK (meal_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK'))";
    static final String ADD_CALORIES = "ALTER TABLE meal_history ADD COLUMN calories INTEGER NOT NULL DEFAULT 0 "
            + "CHECK (calories >= 0)";
    static final String ADD_HEALTHY_LEVEL = "ALTER TABLE meal_history ADD COLUMN healthy_level TEXT NOT NULL DEFAULT 'MODERATE' "
            + "CHECK (healthy_level IN ('HEALTHY', 'MODERATE', 'INDULGENT'))";

    static final String INDEX_DATE = "CREATE INDEX IF NOT EXISTS idx_meal_history_date ON meal_history (plan_date)";

    // ------------------------------------------------------------------ the queries

    /** "Only this user's rows", or every row when the DAO has no user: (? IS NULL OR user_id = ?). */
    private static final String MINE = "(? IS NULL OR user_id = ?)";
    private static final String COLUMNS =
            "id, plan_date, meal_slot, recipe_name, category, prep_minutes, calories, healthy_level";
    /** Breakfast first, then Lunch, Dinner and Snack. */
    private static final String SLOT_ORDER =
            "CASE meal_slot WHEN 'BREAKFAST' THEN 1 WHEN 'LUNCH' THEN 2 WHEN 'DINNER' THEN 3 ELSE 4 END";

    static final String INSERT_MEAL = "INSERT INTO meal_history "
            + "(user_id, plan_date, meal_slot, recipe_name, category, prep_minutes, calories, healthy_level) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    static final String SELECT_BETWEEN = "SELECT " + COLUMNS + " FROM meal_history "
            + "WHERE plan_date BETWEEN ? AND ? AND " + MINE + " ORDER BY plan_date, " + SLOT_ORDER + ", id";
    static final String SELECT_ALL = "SELECT " + COLUMNS + " FROM meal_history "
            + "WHERE " + MINE + " ORDER BY plan_date, " + SLOT_ORDER + ", id";
    static final String SELECT_DATES = "SELECT DISTINCT plan_date FROM meal_history WHERE " + MINE;
    static final String SELECT_CATEGORIES = "SELECT category FROM meal_history WHERE " + MINE + " ORDER BY id";
    static final String SELECT_PREP = "SELECT prep_minutes FROM meal_history WHERE " + MINE + " ORDER BY id";
    static final String COUNT_ALL = "SELECT COUNT(*) FROM meal_history WHERE " + MINE;
    static final String COUNT_CATEGORY = "SELECT COUNT(*) FROM meal_history WHERE category = ? AND " + MINE;
    static final String DELETE_ONE = "DELETE FROM meal_history WHERE id = ? AND " + MINE;
    static final String DELETE_ALL = "DELETE FROM meal_history WHERE " + MINE;
    static final String CLAIM_UNOWNED = "UPDATE meal_history SET user_id = ? WHERE user_id IS NULL";
    static final String INSERT_INGREDIENT = "INSERT OR IGNORE INTO meal_ingredient (recipe_name, ingredient) VALUES (?, ?)";
    static final String SELECT_INGREDIENTS = "SELECT ingredient FROM meal_ingredient WHERE recipe_name = ? ORDER BY ingredient";

    private final Connection conn;
    private final Integer userId;

    /** A DAO that sees every row (tests, and creating the tables at start-up). */
    public MealPlanHistoryDao(Connection conn) {
        this(conn, null);
    }

    /** A DAO for one logged-in user: it only reads and writes that user's meals. */
    public MealPlanHistoryDao(Connection conn, Integer userId) {
        this.conn = conn;
        this.userId = userId;
    }

    /** The user this DAO belongs to, or null if it sees every row. */
    public Integer getUserId() {
        return userId;
    }

    /**
     * Creates the tables (and fast indexes) if they are missing, and upgrades a database file made by
     * an older build. Safe to run every time the app starts.
     */
    public synchronized void initSchema() throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute(CREATE_USERS);
            st.execute(CREATE_MEAL_HISTORY);
            st.execute(CREATE_MEAL_INGREDIENT);
        }
        upgradeOldMealHistory();
        try (Statement st = conn.createStatement()) {
            st.execute(INDEX_DATE);
            // Speeds up bulk imports (thousands of single-row inserts). Fine for course data.
            st.execute("PRAGMA synchronous = OFF");
        }
    }

    /** Adds the columns an old meal_history is missing, then gives the old rows a sensible slot and level. */
    private void upgradeOldMealHistory() throws SQLException {
        Set<String> columns = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement("PRAGMA table_info(meal_history)");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                columns.add(rs.getString("name").toLowerCase());
            }
        }
        boolean slotAdded = !columns.contains("meal_slot");
        boolean levelAdded = !columns.contains("healthy_level");
        try (Statement st = conn.createStatement()) {
            if (!columns.contains("user_id")) {
                st.execute(ADD_USER_ID);
            }
            if (slotAdded) {
                st.execute(ADD_MEAL_SLOT);
            }
            if (!columns.contains("calories")) {
                st.execute(ADD_CALORIES);
            }
            if (levelAdded) {
                st.execute(ADD_HEALTHY_LEVEL);
            }
        }
        if (slotAdded || levelAdded) {
            fillInOldRows(slotAdded, levelAdded);
        }
    }

    /**
     * Old rows only got the column defaults (Dinner, Moderate). This uses the same rules as new rows:
     * the slot comes from the recipe type, the level from HealthClassifier. One transaction.
     */
    private void fillInOldRows(boolean slotAdded, boolean levelAdded) throws SQLException {
        List<Object[]> updates = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, category, calories, meal_slot, healthy_level FROM meal_history");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String category = rs.getString("category");
                MealSlot slot = slotAdded ? MealSlot.defaultFor(category)
                        : MealSlot.fromTextOr(rs.getString("meal_slot"), MealSlot.defaultFor(category));
                HealthyLevel level = levelAdded
                        ? HealthClassifier.classify(Math.max(rs.getInt("calories"), 0), slot, category)
                        : HealthyLevel.fromTextOr(rs.getString("healthy_level"), HealthyLevel.MODERATE);
                updates.add(new Object[] {slot.name(), level.name(), rs.getLong("id")});
            }
        }
        if (updates.isEmpty()) {
            return;
        }
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE meal_history SET meal_slot = ?, healthy_level = ? WHERE id = ?")) {
            for (Object[] u : updates) {
                ps.setString(1, (String) u[0]);
                ps.setString(2, (String) u[1]);
                ps.setLong(3, (Long) u[2]);
                ps.addBatch();
            }
            ps.executeBatch();
            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    /** Fills the two "?" of (? IS NULL OR user_id = ?) starting at index; returns the next free index. */
    private int bindMine(PreparedStatement ps, int index) throws SQLException {
        if (userId == null) {
            ps.setNull(index, Types.INTEGER);
            ps.setNull(index + 1, Types.INTEGER);
        } else {
            ps.setInt(index, userId);
            ps.setInt(index + 1, userId);
        }
        return index + 2;
    }

    // ------------------------------------------------------------------ writing

    public synchronized void insert(ImportRow row) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_MEAL)) {
            if (userId == null) {
                ps.setNull(1, Types.INTEGER);
            } else {
                ps.setInt(1, userId);
            }
            ps.setString(2, row.getDate());
            ps.setString(3, row.getMealSlot().name());
            ps.setString(4, row.getRecipeName());
            ps.setString(5, row.getCategory());
            ps.setInt(6, row.getPrepMinutes());
            ps.setInt(7, row.getCalories());
            ps.setString(8, row.getHealthyLevel().name());
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
        try (PreparedStatement ps = conn.prepareStatement(INSERT_INGREDIENT)) {
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

    /** Removes one meal. A meal of another user is never removed. */
    @Override
    public synchronized void deleteMeal(long id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_ONE)) {
            ps.setLong(1, id);
            bindMine(ps, 2);
            ps.executeUpdate();
        }
    }

    /** Wipes this user's meals (every meal if the DAO has no user). Handy to reset the demo. */
    public synchronized void deleteAll() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_ALL)) {
            bindMine(ps, 1);
            ps.executeUpdate();
        }
    }

    /**
     * Meals saved before accounts existed have no owner. The first person who logs in gets them,
     * so nothing that was planned before this update is lost. Returns how many rows were claimed.
     */
    public synchronized int claimUnownedMeals() throws SQLException {
        if (userId == null) {
            return 0;
        }
        try (PreparedStatement ps = conn.prepareStatement(CLAIM_UNOWNED)) {
            ps.setInt(1, userId);
            return ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ reading

    /** Category of every planned meal - the input of the data-mining step. */
    public synchronized List<String> getPlannedCategories() throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_CATEGORIES)) {
            bindMine(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString("category"));
                }
            }
        }
        return out;
    }

    public synchronized List<Integer> getPrepMinutes() throws SQLException {
        List<Integer> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_PREP)) {
            bindMine(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getInt("prep_minutes"));
                }
            }
        }
        return out;
    }

    /** All meals planned on one date (yyyy-MM-dd). */
    public synchronized List<ImportRow> findByDate(String date) throws SQLException {
        List<ImportRow> out = new ArrayList<>();
        for (PlannedMeal m : queryMeals(date, date)) {
            out.add(new ImportRow(m.getDate(), m.getRecipeName(), m.getCategory(), m.getPrepMinutes(),
                    m.getMealSlot(), m.getCalories(), m.getHealthyLevel()));
        }
        return out;
    }

    public synchronized int count() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(COUNT_ALL)) {
            bindMine(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public synchronized int countByCategory(String category) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(COUNT_CATEGORY)) {
            ps.setString(1, category);
            bindMine(ps, 2);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    private List<PlannedMeal> queryMeals(String from, String to) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BETWEEN)) {
            ps.setString(1, from);
            ps.setString(2, to);
            bindMine(ps, 3);
            return readMeals(ps);
        }
    }

    private static List<PlannedMeal> readMeals(PreparedStatement ps) throws SQLException {
        List<PlannedMeal> out = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                out.add(new PlannedMeal(rs.getLong("id"), rs.getString("plan_date"),
                        rs.getString("recipe_name"), rs.getString("category"), rs.getInt("prep_minutes"),
                        MealSlot.fromTextOr(rs.getString("meal_slot"), null),
                        rs.getInt("calories"),
                        HealthyLevel.fromTextOr(rs.getString("healthy_level"), null)));
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
    public synchronized List<PlannedMeal> allMeals() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ALL)) {
            bindMine(ps, 1);
            return readMeals(ps);
        }
    }

    @Override
    public synchronized Set<LocalDate> plannedDates() throws SQLException {
        Set<LocalDate> out = new HashSet<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_DATES)) {
            bindMine(ps, 1);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        out.add(LocalDate.parse(rs.getString(1)));
                    } catch (DateTimeParseException ignored) {
                        // a badly written date in the table is skipped, never a crash
                    }
                }
            }
        }
        return out;
    }

    @Override
    public synchronized List<String> ingredientsFor(String recipeName) throws SQLException {
        List<String> out = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(SELECT_INGREDIENTS)) {
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
