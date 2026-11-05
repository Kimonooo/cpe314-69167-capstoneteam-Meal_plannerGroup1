package com.platenplatter.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ShoppingListBuilderTest {

    private static ShoppingList.Item find(ShoppingList list, String name) {
        for (ShoppingList.Section s : list.getSections()) {
            for (ShoppingList.Item i : s.getItems()) {
                if (i.getName().equals(name)) {
                    return i;
                }
            }
        }
        return null;
    }

    @Test
    void sameIngredientFromTwoMealsBecomesOneLineWithACount() {
        ShoppingList list = ShoppingListBuilder.build(List.of(
                new MealIngredients("Chicken Adobo", 1, List.of("Chicken", "Garlic", "Soy sauce")),
                new MealIngredients("Arroz Caldo", 1, List.of("Chicken", "Garlic", "Rice"))));
        ShoppingList.Item garlic = find(list, "Garlic");
        assertNotNull(garlic);
        assertEquals(2, garlic.getMealCount());
        assertEquals(List.of("Arroz Caldo", "Chicken Adobo"), garlic.getRecipes());
        assertEquals("for 2 meals", garlic.note());
        assertEquals(4, list.itemCount()); // chicken, garlic, soy sauce, rice
    }

    @Test
    void aRecipePlannedTwiceCountsTwice() {
        ShoppingList list = ShoppingListBuilder.build(List.of(new MealIngredients("Tapsilog", 2, List.of("Egg", "Rice"))));
        assertEquals(2, find(list, "Egg").getMealCount());
        assertEquals(2, list.getMealCount());
    }

    @Test
    void differentSpellingsAndDuplicatesInOneRecipeAreMerged() {
        ShoppingList list = ShoppingListBuilder.build(List.of(
                new MealIngredients("A", 1, List.of("Eggs", "egg ", "EGG"))));
        assertEquals(1, list.itemCount());
        assertEquals(1, find(list, "Egg").getMealCount());
    }

    @Test
    void sectionsFollowTheShopOrderAndItemsAreAlphabetical() {
        ShoppingList list = ShoppingListBuilder.build(List.of(
                new MealIngredients("A", 1, List.of("Salt", "Rice", "Zucchini", "Apple", "Chicken", "Milk"))));
        assertEquals(List.of(Aisle.PRODUCE, Aisle.MEAT, Aisle.DAIRY, Aisle.PANTRY, Aisle.SPICES),
                list.getSections().stream().map(ShoppingList.Section::getAisle).toList());
        assertEquals("Apple", list.getSections().get(0).getItems().get(0).getName());
        assertEquals("Zucchini", list.getSections().get(0).getItems().get(1).getName());
    }

    @Test
    void recipesWithoutIngredientsAreReportedAsMissing() {
        ShoppingList list = ShoppingListBuilder.build(List.of(
                new MealIngredients("Mystery Stew", 1, List.of()),
                new MealIngredients("Blank", 1, List.of("  ", ""))));
        assertTrue(list.isEmpty());
        assertEquals(List.of("Blank", "Mystery Stew"), list.getMissingRecipes());
    }

    @Test
    void emptyInputGivesAnEmptyList() {
        ShoppingList list = ShoppingListBuilder.build(List.of());
        assertTrue(list.isEmpty());
        assertEquals(0, list.itemCount());
        assertEquals(0, list.getMealCount());
    }

    @Test
    void plainTextShowsAislesTickBoxesAndMissingRecipes() {
        ShoppingList list = ShoppingListBuilder.build(List.of(
                new MealIngredients("Chicken Adobo", 1, List.of("Chicken", "Garlic")),
                new MealIngredients("Mystery", 1, List.of())));
        String text = list.toPlainText("My list", Set.of("garlic"));
        assertTrue(text.startsWith("My list\n2 meals, 2 items"));
        assertTrue(text.contains("FRUITS & VEGETABLES\n[x] Garlic"));
        assertTrue(text.contains("MEAT & SEAFOOD\n[ ] Chicken"));
        assertTrue(text.contains("No ingredient list yet for: Mystery"));
    }
}
