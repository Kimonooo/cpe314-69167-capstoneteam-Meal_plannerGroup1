package com.platenplatter.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Built-in ingredient lists for the dishes in our sample CSV files, so the shopping list works even
 * for meals that were imported (a CSV has no ingredients). Recipes added from the Recipes tab bring
 * their own ingredients from TheMealDB and do not need this.
 */
public final class SampleIngredients {
    private static final Map<String, List<String>> DISHES = new HashMap<>();

    private SampleIngredients() { }

    private static void dish(String name, String... ingredients) {
        DISHES.put(name.toLowerCase(Locale.ROOT), List.of(ingredients));
    }

    static {
        // Breakfast
        dish("Tapsilog", "Beef tapa", "Garlic", "Rice", "Egg", "Cooking oil", "Soy sauce", "Vinegar", "Black pepper");
        dish("Champorado", "Glutinous rice", "Cocoa powder", "Sugar", "Evaporated milk", "Water", "Salt");
        dish("Pancakes", "Flour", "Egg", "Milk", "Sugar", "Baking powder", "Butter", "Maple syrup", "Salt");
        dish("Garlic Fried Rice and Egg", "Rice", "Garlic", "Egg", "Cooking oil", "Salt", "Spring onion");
        dish("Oatmeal with Banana", "Oats", "Banana", "Milk", "Honey", "Cinnamon");
        dish("Rice, Egg and Longganisa", "Longganisa", "Rice", "Egg", "Garlic", "Cooking oil", "Vinegar");
        dish("Pandesal and Coffee", "Pandesal", "Coffee", "Butter", "Sugar", "Milk");
        dish("Arroz Caldo", "Glutinous rice", "Chicken", "Ginger", "Garlic", "Onion", "Fish sauce", "Egg", "Spring onion", "Calamansi");
        // Main courses
        dish("Chicken Adobo", "Chicken", "Soy sauce", "Vinegar", "Garlic", "Bay leaves", "Black pepper", "Cooking oil", "Sugar");
        dish("Sinigang na Baboy", "Pork belly", "Tamarind", "Tomato", "Onion", "Kangkong", "Radish", "Long beans", "Fish sauce", "Green chili", "Water");
        dish("Beef Caldereta", "Beef", "Tomato sauce", "Potato", "Carrot", "Bell pepper", "Onion", "Garlic", "Liver spread", "Cheese", "Cooking oil", "Black pepper");
        dish("Pancit Canton", "Egg noodles", "Chicken", "Cabbage", "Carrot", "Garlic", "Onion", "Soy sauce", "Oyster sauce", "Cooking oil", "Calamansi");
        dish("Fried Bangus", "Bangus", "Garlic", "Vinegar", "Salt", "Black pepper", "Cooking oil", "Tomato");
        dish("Chicken Tinola", "Chicken", "Ginger", "Onion", "Green papaya", "Chili leaves", "Fish sauce", "Water", "Garlic");
        dish("Kare-Kare", "Oxtail", "Peanut butter", "Eggplant", "String beans", "Bok choy", "Annatto", "Garlic", "Onion", "Shrimp paste", "Rice");
        dish("Bicol Express", "Pork belly", "Coconut milk", "Green chili", "Shrimp paste", "Garlic", "Onion", "Ginger");
        dish("Lechon Kawali", "Pork belly", "Salt", "Black pepper", "Bay leaves", "Garlic", "Cooking oil", "Vinegar", "Soy sauce");
        // Desserts
        dish("Leche Flan", "Egg", "Condensed milk", "Evaporated milk", "Sugar", "Vanilla extract");
        dish("Halo-Halo", "Ice", "Evaporated milk", "Sweet beans", "Banana", "Jackfruit", "Macapuno", "Leche flan", "Ube", "Ice cream", "Sugar");
        dish("Buko Pandan", "Young coconut", "Pandan", "Gelatin", "Condensed milk", "Cream", "Sugar");
        dish("Banana Cue", "Saba banana", "Brown sugar", "Cooking oil", "Bamboo skewers");
        dish("Mango Float", "Mango", "Graham crackers", "Cream", "Condensed milk");
        dish("Turon", "Saba banana", "Jackfruit", "Spring roll wrapper", "Brown sugar", "Cooking oil");
        dish("Bibingka", "Rice flour", "Coconut milk", "Egg", "Sugar", "Baking powder", "Butter", "Salted egg", "Cheese", "Banana leaves");
    }

    /** The built-in ingredients of a dish (matching ignores capital letters), or an empty list. */
    public static List<String> forRecipe(String recipeName) {
        if (recipeName == null) {
            return new ArrayList<>();
        }
        List<String> found = DISHES.get(recipeName.trim().toLowerCase(Locale.ROOT));
        return found == null ? new ArrayList<>() : new ArrayList<>(found);
    }
}
