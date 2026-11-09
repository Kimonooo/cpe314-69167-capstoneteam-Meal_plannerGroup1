package com.platenplatter.model;

/**
 * One row of the ingredient_amounts table: one ingredient with its measure in one saved recipe,
 * for example 3 cloves of garlic in Spicy Arrabiata Penne.
 * It is the bridge between saved_recipes and ingredients.
 */
public class IngredientAmount {
    private final long ingredientAmountId;
    private final long savedRecipeId;
    private final Ingredient ingredient;
    private final String measure;

    public IngredientAmount(long ingredientAmountId, long savedRecipeId, Ingredient ingredient, String measure) {
        if (ingredient == null) {
            throw new IllegalArgumentException("An ingredient amount needs an ingredient");
        }
        this.ingredientAmountId = ingredientAmountId;
        this.savedRecipeId = savedRecipeId;
        this.ingredient = ingredient;
        this.measure = measure == null ? "" : measure.trim();
    }

    /** An amount read from TheMealDB, not saved yet: "garlic" and "3 cloves". */
    public IngredientAmount(String ingredientName, String measure) {
        this(0, 0, Ingredient.named(ingredientName), measure);
    }

    public long getIngredientAmountId() { return ingredientAmountId; }
    public long getSavedRecipeId() { return savedRecipeId; }
    public Ingredient getIngredient() { return ingredient; }
    public String getMeasure() { return measure; }

    /** "3 cloves garlic", or just "garlic" when there is no measure. */
    public String label() {
        return measure.isEmpty() ? ingredient.getIngredientName() : measure + " " + ingredient.getIngredientName();
    }

    @Override
    public String toString() {
        return label();
    }
}
