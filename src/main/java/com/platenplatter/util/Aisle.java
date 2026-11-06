package com.platenplatter.util;

/** The sections of a shop, in the order we walk through them. */
public enum Aisle {
    PRODUCE("Fruits & Vegetables"),
    MEAT("Meat & Seafood"),
    DAIRY("Dairy & Eggs"),
    BAKERY("Bakery"),
    PANTRY("Pantry & Grains"),
    SAUCES("Sauces & Condiments"),
    SPICES("Spices & Seasonings"),
    OTHER("Other");

    private final String label;

    Aisle(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
