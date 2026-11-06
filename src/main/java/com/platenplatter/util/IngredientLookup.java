package com.platenplatter.util;

import com.platenplatter.api.MealData;
import com.platenplatter.api.MealDbClient;

import java.util.List;

/** "Look up online": asks TheMealDB for the ingredients of recipes we know only by name. */
public final class IngredientLookup {
    private IngredientLookup() { }

    /**
     * For each name, searches TheMealDB. Only an EXACT name match (ignoring capital letters) is saved,
     * so we never attach the wrong ingredients to a dish.
     * @return how many recipes were found and saved
     */
    public static int fill(PlanStore store, MealDbClient client, List<String> recipeNames) throws Exception {
        int found = 0;
        for (String name : recipeNames) {
            for (MealData candidate : client.searchByName(name)) {
                if (candidate.getName().equalsIgnoreCase(name) && !candidate.getIngredients().isEmpty()) {
                    store.saveIngredients(name, candidate.getIngredients());
                    found++;
                    break;
                }
            }
        }
        return found;
    }
}
