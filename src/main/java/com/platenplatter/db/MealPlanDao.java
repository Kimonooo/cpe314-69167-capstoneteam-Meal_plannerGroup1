package com.platenplatter.db;

import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.Ingredient;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.util.PlanStore;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

/**
 * All SQL for saved_recipes, ingredients, ingredient_amounts and planned_meals, for ONE user.
 * The DAO is made at log-in with that user's id and binds it as the first "?" of every query,
 * so a user only ever sees their own rows. Every query uses PreparedStatement.
 * The methods are synchronized because the CSV import writes while the pages read.
 */
public class MealPlanDao implements PlanStore {

    private static final String MEAL_COLUMNS = """
            p.planned_meal_id, p.user_id, p.plan_date, p.meal_slot, p.prep_minutes,
            p.calories, p.healthy_level, r.saved_recipe_id, r.recipe_name, r.meal_type,
            r.cuisine, r.base_prep_minutes, r.usual_calories, r.mealdb_id""";

    static final String MEALS_BETWEEN = "SELECT " + MEAL_COLUMNS + """

            FROM planned_meals p
            JOIN saved_recipes r ON r.saved_recipe_id = p.saved_recipe_id
            WHERE p.user_id = ? AND p.plan_date BETWEEN ? AND ?
            ORDER BY p.plan_date, p.planned_meal_id""";

    static final String ALL_MEALS = "SELECT " + MEAL_COLUMNS + """

            FROM planned_meals p
            JOIN saved_recipes r ON r.saved_recipe_id = p.saved_recipe_id
            WHERE p.user_id = ?
            ORDER BY p.plan_date, p.planned_meal_id""";

    static final String SHOPPING_ITEMS = """
            SELECT a.ingredient_amount_id, a.saved_recipe_id, a.measure,
                   i.ingredient_id, i.ingredient_name, i.food_group
            FROM planned_meals p
            JOIN ingredient_amounts a ON a.saved_recipe_id = p.saved_recipe_id
            JOIN ingredients i ON i.ingredient_id = a.ingredient_id
            WHERE p.user_id = ? AND p.plan_date BETWEEN ? AND ?
            ORDER BY i.ingredient_name""";

    /** One saved recipe per name: planning it again only updates its usual calories. */
    private static final String UPSERT_RECIPE = """
            INSERT INTO saved_recipes (user_id, recipe_name, meal_type, cuisine,
                                       base_prep_minutes, usual_calories, mealdb_id)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT (user_id, recipe_name) DO UPDATE SET usual_calories =
                CASE WHEN excluded.usual_calories > 0 THEN excluded.usual_calories ELSE usual_calories END""";

    private static final String FIND_RECIPE =
            "SELECT saved_recipe_id, recipe_name, meal_type, cuisine, base_prep_minutes, usual_calories, mealdb_id "
            + "FROM saved_recipes WHERE user_id = ? AND recipe_name = ?";

    private static final String INSERT_INGREDIENT =
            "INSERT OR IGNORE INTO ingredients (ingredient_name, food_group) VALUES (?, ?)";

    private static final String INSERT_AMOUNT = """
            INSERT OR IGNORE INTO ingredient_amounts (saved_recipe_id, ingredient_id, measure)
            SELECT ?, ingredient_id, ? FROM ingredients WHERE ingredient_name = ?""";

    private static final String INSERT_MEAL = """
            INSERT INTO planned_meals (user_id, saved_recipe_id, plan_date, meal_slot,
                                       prep_minutes, calories, healthy_level)
            VALUES (?, ?, ?, ?, ?, ?, ?)""";

    /** Reads one row of a ResultSet into an object. */
    @FunctionalInterface
    private interface RowReader<T> {
        T read(ResultSet rs) throws SQLException;
    }

    private final Connection conn;
    private final long userId;

    public MealPlanDao(Connection conn, long userId) {
        this.conn = conn;
        this.userId = userId;
    }

    public long getUserId() {
        return userId;
    }

    // ------------------------------------------------------------------ writing

    /** Saves the recipe (once per name), its ingredients and the meal, all in one transaction. */
    @Override
    public synchronized long saveMeal(PlannedMeal meal, List<IngredientAmount> ingredients) throws SQLException {
        return inTransaction(() -> {
            long savedRecipeId = saveRecipe(meal);
            ingredients.forEach(amount -> saveAmount(savedRecipeId, amount));
            try (PreparedStatement ps = conn.prepareStatement(INSERT_MEAL)) {
                ps.setLong(1, userId);
                ps.setLong(2, savedRecipeId);
                ps.setString(3, meal.getPlanDate().toString());
                ps.setString(4, meal.getMealSlot().name());
                ps.setInt(5, meal.getPrepMinutes());
                ps.setInt(6, meal.getCalories());
                ps.setString(7, meal.getHealthyLevel().name());
                ps.executeUpdate();
            }
            return lastId();
        });
    }

    @Override
    public synchronized void saveIngredients(String recipeName, List<IngredientAmount> amounts) throws SQLException {
        inTransaction(() -> {
            long savedRecipeId = findRecipe(recipeName).map(SavedRecipe::getSavedRecipeId).orElse(0L);
            if (savedRecipeId > 0) {
                amounts.forEach(amount -> saveAmount(savedRecipeId, amount));
            }
            return savedRecipeId;
        });
    }

    @Override
    public synchronized void deleteMeal(long plannedMealId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM planned_meals WHERE planned_meal_id = ? AND user_id = ?")) {
            ps.setLong(1, plannedMealId);
            ps.setLong(2, userId);                           // never another user's meal
            ps.executeUpdate();
        }
    }

    // ------------------------------------------------------------------ reading

    @Override
    public synchronized List<PlannedMeal> mealsBetween(LocalDate from, LocalDate to)
            throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(MEALS_BETWEEN)) {
            ps.setLong(1, userId);                 // only this user's meals
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            return readAll(ps, MealPlanDao::toPlannedMeal);
        }
    }

    @Override
    public synchronized List<PlannedMeal> allMeals() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(ALL_MEALS)) {
            ps.setLong(1, userId);
            return readAll(ps, MealPlanDao::toPlannedMeal);
        }
    }

    @Override
    public synchronized Set<LocalDate> plannedDates() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT DISTINCT plan_date FROM planned_meals WHERE user_id = ? ORDER BY plan_date")) {
            ps.setLong(1, userId);
            return new LinkedHashSet<>(readAll(ps, rs -> LocalDate.parse(rs.getString(1))));
        }
    }

    @Override
    public synchronized List<IngredientAmount> shoppingItems(LocalDate from, LocalDate to) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SHOPPING_ITEMS)) {
            ps.setLong(1, userId);
            ps.setString(2, from.toString());
            ps.setString(3, to.toString());
            return readAll(ps, rs -> new IngredientAmount(rs.getLong("ingredient_amount_id"), rs.getLong("saved_recipe_id"),
                    new Ingredient(rs.getLong("ingredient_id"), rs.getString("ingredient_name"),
                            FoodGroup.fromName(rs.getString("food_group"))),
                    rs.getString("measure")));
        }
    }

    @Override
    public synchronized OptionalInt usualCalories(String recipeName) throws SQLException {
        return findRecipe(recipeName)
                .filter(recipe -> recipe.getUsualCalories() > 0)
                .map(recipe -> OptionalInt.of(recipe.getUsualCalories()))
                .orElse(OptionalInt.empty());
    }

    /** The saved recipe with this name, ignoring upper/lower case. */
    public synchronized Optional<SavedRecipe> findRecipe(String recipeName) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FIND_RECIPE)) {
            ps.setLong(1, userId);
            ps.setString(2, recipeName == null ? "" : recipeName.trim());
            return readAll(ps, MealPlanDao::toRecipe).stream().findFirst();
        }
    }

    // ------------------------------------------------------------------ helpers

    private long saveRecipe(PlannedMeal meal) throws SQLException {
        SavedRecipe recipe = meal.getRecipe();
        try (PreparedStatement ps = conn.prepareStatement(UPSERT_RECIPE)) {
            ps.setLong(1, userId);
            ps.setString(2, recipe.getRecipeName());
            ps.setString(3, recipe.getMealType());
            ps.setString(4, recipe.getCuisine());
            ps.setInt(5, recipe.getBasePrepMinutes());
            ps.setInt(6, meal.getCalories());             // remembered as usual_calories
            ps.setString(7, recipe.getMealDbId());
            ps.executeUpdate();
        }
        return findRecipe(recipe.getRecipeName()).map(SavedRecipe::getSavedRecipeId)
                .orElseThrow(() -> new SQLException("The recipe was not saved: " + recipe.getRecipeName()));
    }

    /** Saves one ingredient (once for the whole app) and its amount in one recipe (once per recipe). */
    private void saveAmount(long savedRecipeId, IngredientAmount amount) {
        Ingredient ingredient = amount.getIngredient();
        try (PreparedStatement addIngredient = conn.prepareStatement(INSERT_INGREDIENT);
             PreparedStatement addAmount = conn.prepareStatement(INSERT_AMOUNT)) {
            addIngredient.setString(1, ingredient.getIngredientName());
            addIngredient.setString(2, ingredient.getFoodGroup().name());
            addIngredient.executeUpdate();
            addAmount.setLong(1, savedRecipeId);
            addAmount.setString(2, amount.getMeasure());
            addAmount.setString(3, ingredient.getIngredientName());
            addAmount.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save the ingredient " + ingredient.getIngredientName(), e);
        }
    }

    /** Runs the work as one transaction: everything is saved, or nothing is. */
    private <T> T inTransaction(SqlWork<T> work) throws SQLException {
        boolean oldAutoCommit = conn.getAutoCommit();
        conn.setAutoCommit(false);
        try {
            T result = work.run();
            conn.commit();
            return result;
        } catch (SQLException | RuntimeException e) {
            conn.rollback();
            throw e instanceof SQLException sql ? sql : new SQLException(e.getMessage(), e);
        } finally {
            conn.setAutoCommit(oldAutoCommit);
        }
    }

    @FunctionalInterface
    private interface SqlWork<T> {
        T run() throws SQLException;
    }

    private long lastId() throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT last_insert_rowid()"); ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    /** Reads every row of a query. The one loop in the app: JDBC can only be read row by row. */
    private static <T> List<T> readAll(PreparedStatement ps, RowReader<T> reader) throws SQLException {
        List<T> rows = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                rows.add(reader.read(rs));
            }
        }
        return rows;
    }

    private static SavedRecipe toRecipe(ResultSet rs) throws SQLException {
        return SavedRecipe.create(rs.getString("meal_type"), rs.getLong("saved_recipe_id"), rs.getString("recipe_name"),
                rs.getString("cuisine"), rs.getInt("base_prep_minutes"), rs.getInt("usual_calories"),
                rs.getString("mealdb_id"));
    }

    private static PlannedMeal toPlannedMeal(ResultSet rs) throws SQLException {
        return new PlannedMeal(rs.getLong("planned_meal_id"), rs.getLong("user_id"), toRecipe(rs),
                LocalDate.parse(rs.getString("plan_date")),
                MealSlot.fromTextOr(rs.getString("meal_slot"), MealSlot.DINNER),
                rs.getInt("prep_minutes"), rs.getInt("calories"),
                HealthyLevel.fromTextOr(rs.getString("healthy_level"), HealthyLevel.MODERATE));
    }
}
