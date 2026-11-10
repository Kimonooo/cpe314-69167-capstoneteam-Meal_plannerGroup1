package com.platenplatter.model;

/**
 * One row of the ingredients table: an ingredient name, saved once for the whole app,
 * with the food group it is shown under on the shopping list.
 */
public class Ingredient {
    private final long ingredientId;
    private final String ingredientName;
    private final FoodGroup foodGroup;

    public Ingredient(long ingredientId, String ingredientName, FoodGroup foodGroup) {
        if (ingredientName == null || ingredientName.isBlank()) {
            throw new IllegalArgumentException("Ingredient name cannot be empty");
        }
        this.ingredientId = ingredientId;
        this.ingredientName = ingredientName.trim();
        this.foodGroup = foodGroup == null ? FoodGroup.OTHER : foodGroup;
    }

    /** A new ingredient (not saved yet, so id 0); its food group is picked from its name. */
    public static Ingredient named(String ingredientName) {
        return new Ingredient(0, ingredientName, FoodGroup.of(ingredientName));
    }

    public long getIngredientId() { return ingredientId; }
    public String getIngredientName() { return ingredientName; }
    public FoodGroup getFoodGroup() { return foodGroup; }

    @Override
    public String toString() {
        return ingredientName;
    }
}
