package com.platenplatter.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.model.DessertRecipe;
import com.platenplatter.model.FoodGroup;
import com.platenplatter.model.HealthyLevel;
import com.platenplatter.model.IngredientAmount;
import com.platenplatter.model.MealSlot;
import com.platenplatter.model.PlannedMeal;
import com.platenplatter.model.SavedRecipe;
import com.platenplatter.model.User;
import com.platenplatter.service.AuthService;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MealPlanDaoTest {
    private static final LocalDate MONDAY = LocalDate.of(2026, 11, 2);

    private Connection conn;
    private MealPlanDao dao;

    @BeforeEach
    void setUp() throws Exception {
        conn = DatabaseConnector.open("jdbc:sqlite::memory:");
        DatabaseConnector.createTables(conn);
        User kim = new UserDao(conn).insertUser(new User(0, "kim", "Kim", 20, "F", AuthService.SECURITY_QUESTIONS.get(0)),
                "secret1", "bantay");
        dao = new MealPlanDao(conn, kim.getUserId());
    }

    @AfterEach
    void tearDown() throws SQLException {
        conn.close();
    }

    private static PlannedMeal meal(String name, String mealType, LocalDate date, int prep, int calories) {
        return new PlannedMeal(0, 0, SavedRecipe.create(mealType, 0, name, "Filipino", 30, calories, ""), date,
                MealSlot.DINNER, prep, calories, HealthyLevel.HEALTHY);
    }

    private int count(String table) throws SQLException {
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery("SELECT count(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }

    @Test
    void aSavedMealIsReadBackWithItsRecipe() throws Exception {
        long id = dao.saveMeal(meal("Leche Flan", "DESSERT", MONDAY, 50, 350), List.of());
        PlannedMeal back = dao.mealsOn(MONDAY).get(0);
        assertEquals(id, back.getPlannedMealId());
        assertEquals(dao.getUserId(), back.getUserId());
        assertEquals("Leche Flan", back.getRecipeName());
        assertInstanceOf(DessertRecipe.class, back.getRecipe());       // meal_type picks the subclass
        assertEquals(45, back.getRecipe().getPrepMinutes());          // base 30 + 15
        assertEquals(50, back.getPrepMinutes());
        assertEquals(350, back.getCalories());
        assertEquals(HealthyLevel.HEALTHY, back.getHealthyLevel());
    }

    @Test
    void planningTheSameRecipeTwiceKeepsOneSavedRecipeRow() throws Exception {
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY, 45, 620), List.of());
        dao.saveMeal(meal("chicken adobo", "MAIN_COURSE", MONDAY.plusDays(3), 45, 0), List.of());
        assertEquals(1, count("saved_recipes"));
        assertEquals(2, count("planned_meals"));
    }

    @Test
    void usualCaloriesFollowTheLastSaveButZeroKeepsTheOldValue() throws Exception {
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY, 45, 620), List.of());
        assertEquals(OptionalInt.of(620), dao.usualCalories("CHICKEN ADOBO"));
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY, 45, 0), List.of());
        assertEquals(OptionalInt.of(620), dao.usualCalories("Chicken Adobo"));
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY, 45, 700), List.of());
        assertEquals(OptionalInt.of(700), dao.usualCalories("Chicken Adobo"));
        assertFalse(dao.usualCalories("Never planned").isPresent());
    }

    @Test
    void ingredientsAreSavedOnceAndLinkedByIds() throws Exception {
        List<IngredientAmount> amounts = List.of(new IngredientAmount("garlic", "3 cloves"), new IngredientAmount("chicken", "1 kg"));
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY, 45, 620), amounts);
        dao.saveMeal(meal("Chicken Adobo", "MAIN_COURSE", MONDAY.plusDays(1), 45, 620), amounts);
        dao.saveMeal(meal("Garlic Rice", "BREAKFAST", MONDAY, 15, 300), List.of(new IngredientAmount("Garlic", "5 cloves")));
        assertEquals(2, count("ingredients"));              // "garlic" and "Garlic" are one ingredient
        assertEquals(3, count("ingredient_amounts"));
        assertEquals(5, dao.shoppingItems(MONDAY, MONDAY.plusDays(6)).size());    // 2 + 2 + 1 meals' amounts
        assertEquals(FoodGroup.MEAT_SEAFOOD, dao.shoppingItems(MONDAY, MONDAY).get(0).getIngredient().getFoodGroup());
    }

    @Test
    void ingredientsFoundOnlineAreAddedToAPlannedRecipe() throws Exception {
        dao.saveMeal(meal("Chicken Tinola", "MAIN_COURSE", MONDAY, 40, 380), List.of());
        assertTrue(dao.shoppingItems(MONDAY, MONDAY).isEmpty());
        dao.saveIngredients("chicken tinola", List.of(new IngredientAmount("ginger", "1 thumb")));
        dao.saveIngredients("Not planned", List.of(new IngredientAmount("salt", "1 tsp")));
        assertEquals(1, dao.shoppingItems(MONDAY, MONDAY).size());
    }

    @Test
    void aRangeIncludesBothEndDaysAndIsSortedByDate() throws Exception {
        dao.saveMeal(meal("B", "MAIN_COURSE", MONDAY.plusDays(6), 10, 0), List.of());
        dao.saveMeal(meal("A", "MAIN_COURSE", MONDAY, 10, 0), List.of());
        dao.saveMeal(meal("C", "MAIN_COURSE", MONDAY.plusDays(7), 10, 0), List.of());
        assertEquals(List.of("A", "B"), dao.mealsBetween(MONDAY, MONDAY.plusDays(6)).stream().map(PlannedMeal::getRecipeName).toList());
        assertEquals(3, dao.allMeals().size());
        assertEquals(3, dao.plannedDates().size());
    }

    @Test
    void deletingAMealRemovesOnlyThatMeal() throws Exception {
        long first = dao.saveMeal(meal("A", "MAIN_COURSE", MONDAY, 10, 0), List.of());
        dao.saveMeal(meal("B", "MAIN_COURSE", MONDAY, 10, 0), List.of());
        dao.deleteMeal(first);
        assertEquals(List.of("B"), dao.allMeals().stream().map(PlannedMeal::getRecipeName).toList());
        assertEquals(2, count("saved_recipes"));            // the recipe stays saved for next time
    }

    @Test
    void theDatabaseRejectsNegativePrepEvenWithoutJava() {
        assertThrows(SQLException.class, () -> {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("INSERT INTO planned_meals (user_id, saved_recipe_id, plan_date, meal_slot, prep_minutes, "
                        + "calories, healthy_level) VALUES (1, 1, '2026-11-02', 'DINNER', -5, 0, 'HEALTHY')");
            }
        });
    }

    @Test
    void theDatabaseRejectsAnUnknownSlotOrLevel() {
        assertThrows(SQLException.class, () -> {
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("INSERT INTO saved_recipes (user_id, recipe_name, meal_type) VALUES (1, 'X', 'BRUNCH')");
            }
        });
    }

    @Test
    void aFailedSaveLeavesNothingBehind() throws Exception {
        PlannedMeal bad = new PlannedMeal(0, 0, SavedRecipe.create("MAIN_COURSE", 0, "Ghost", "", 10, 0, ""),
                MONDAY, MealSlot.DINNER, 10, 0, HealthyLevel.HEALTHY);
        try (Statement st = conn.createStatement()) {
            st.execute("DROP TABLE planned_meals");          // make the last step of saveMeal fail
        }
        assertThrows(SQLException.class, () -> dao.saveMeal(bad, List.of(new IngredientAmount("salt", "1 tsp"))));
        assertEquals(0, count("saved_recipes"));            // the transaction was rolled back
        assertEquals(0, count("ingredients"));
    }
}
