package com.platenplatter.util;

import com.platenplatter.model.SavedRecipe;

/** Thrown when a recipe would end up inside itself, for example a cake inside its own filling. */
public class CircularRecipeException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public CircularRecipeException(SavedRecipe parent, SavedRecipe sub) {
        super("Circular reference: \"" + sub.getRecipeName() + "\" already contains \""
                + parent.getRecipeName() + "\", so it cannot go inside it");
    }
}
