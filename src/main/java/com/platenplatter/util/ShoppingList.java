package com.platenplatter.util;

import java.util.List;
import java.util.Set;

/** The finished shopping list: items grouped by aisle, plus the recipes that had no ingredient list. */
public class ShoppingList {

    /** One thing to buy. */
    public static class Item {
        private final String key;
        private final String name;
        private final Aisle aisle;
        private final int mealCount;
        private final List<String> recipes;

        public Item(String key, String name, Aisle aisle, int mealCount, List<String> recipes) {
            this.key = key;
            this.name = name;
            this.aisle = aisle;
            this.mealCount = mealCount;
            this.recipes = recipes;
        }

        /** The cleaned lower-case name; used to remember which items are ticked. */
        public String getKey() { return key; }
        public String getName() { return name; }
        public Aisle getAisle() { return aisle; }
        /** How many planned meals need this item. */
        public int getMealCount() { return mealCount; }
        public List<String> getRecipes() { return recipes; }

        /** For example "for 3 meals" or "Chicken Adobo". */
        public String note() {
            if (recipes.size() == 1 && mealCount == 1) {
                return recipes.get(0);
            }
            return "for " + mealCount + (mealCount == 1 ? " meal" : " meals");
        }
    }

    /** All the items of one aisle. */
    public static class Section {
        private final Aisle aisle;
        private final List<Item> items;

        public Section(Aisle aisle, List<Item> items) {
            this.aisle = aisle;
            this.items = items;
        }

        public Aisle getAisle() { return aisle; }
        public List<Item> getItems() { return items; }
    }

    private final List<Section> sections;
    private final List<String> missingRecipes;
    private final int mealCount;

    public ShoppingList(List<Section> sections, List<String> missingRecipes, int mealCount) {
        this.sections = sections;
        this.missingRecipes = missingRecipes;
        this.mealCount = mealCount;
    }

    public List<Section> getSections() { return sections; }

    /** Recipes that are planned but have no ingredient list yet. */
    public List<String> getMissingRecipes() { return missingRecipes; }

    /** How many planned meals the list was built from. */
    public int getMealCount() { return mealCount; }

    public int itemCount() {
        int n = 0;
        for (Section s : sections) {
            n += s.items.size();
        }
        return n;
    }

    public boolean isEmpty() {
        return sections.isEmpty();
    }

    /** A plain-text version to paste into a chat or print. checkedKeys are the ticked items. */
    public String toPlainText(String title, Set<String> checkedKeys) {
        StringBuilder sb = new StringBuilder();
        sb.append(title).append('\n');
        sb.append(mealCount).append(mealCount == 1 ? " meal, " : " meals, ")
                .append(itemCount()).append(itemCount() == 1 ? " item" : " items").append("\n");
        for (Section section : sections) {
            sb.append('\n').append(section.aisle.label().toUpperCase()).append('\n');
            for (Item item : section.items) {
                boolean done = checkedKeys != null && checkedKeys.contains(item.key);
                sb.append(done ? "[x] " : "[ ] ").append(item.name).append("  (").append(item.note()).append(")\n");
            }
        }
        if (!missingRecipes.isEmpty()) {
            sb.append("\nNo ingredient list yet for: ").append(String.join(", ", missingRecipes)).append('\n');
        }
        return sb.toString();
    }
}
