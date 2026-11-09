package com.platenplatter.model;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * The groups of the shopping list, in the order they are shown.
 * FoodGroup.of(name) picks the group of an ingredient by the words in its name.
 * The group is saved once per ingredient, in ingredients.food_group.
 */
public enum FoodGroup {
    FRUITS_VEGETABLES("Fruits & Vegetables"),
    MEAT_SEAFOOD("Meat & Seafood"),
    DAIRY_EGGS("Dairy & Eggs"),
    BREAD_BAKERY("Bread & Bakery"),
    GRAINS_PANTRY("Grains, Pasta & Pantry"),
    SAUCES_CONDIMENTS("Sauces & Condiments"),
    HERBS_SPICES("Herbs & Spices"),
    OTHER("Other");

    private final String label;

    FoodGroup(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /** "bell pepper" is a vegetable, but a bare "pepper" is the spice. */
    private static final Pattern VEGETABLE_PEPPER = Pattern.compile("\\b(bell|red|green|yellow|chili|chilli|hot) peppers?\\b");

    /**
     * The rules, checked from top to bottom; the first rule whose words appear in the name wins.
     * The order matters: "soy sauce" must hit Sauces before "soy" could hit anything else,
     * and "coconut milk" must hit Grains & Pantry before "milk" hits Dairy.
     */
    private static final List<Map.Entry<FoodGroup, Pattern>> RULES = List.of(
            rule(SAUCES_CONDIMENTS, "sauce|vinegar|ketchup|mayo|mayonnaise|mustard|paste|syrup|honey|dressing|marinade"
                    + "|relish|jam|soy"),
            rule(GRAINS_PANTRY, "stock|broth|bouillon|coconut milk|coconut cream|peanut butter|noodles?|pasta|penne"
                    + "|spaghetti|macaroni|vermicelli|lasagne|lasagna|flour|baking powder|baking soda|cocoa powder"
                    + "|cornstarch|corn starch"),
            rule(HERBS_SPICES, "powder|salt|peppercorns?|pepper|paprika|cumin|turmeric|cinnamon|oregano|thyme|rosemary"
                    + "|bay leaf|bay leaves|nutmeg|curry|vanilla|seasoning|spices?|cayenne|saffron|cardamom|chil+i flakes"
                    + "|ground cloves|basil|parsley|coriander|cilantro|mint|dill|chives"),
            rule(DAIRY_EGGS, "milk|cheese|butter|cream|yogh?urt|eggs?|mozzarella|parmesan|parmigiano|cheddar|margarine"
                    + "|ghee|custard"),
            rule(MEAT_SEAFOOD, "chicken|beef|pork|bacon|ham|sausages?|lamb|turkey|fish|salmon|tuna|shrimps?|prawns?|crab"
                    + "|squid|bangus|milkfish|longganisa|tocino|liver|duck|mutton|anchov(y|ies)|sardines?|mussels?"
                    + "|clams?|oysters?|tilapia|cod|ribs|steak|mince|oxtail"),
            rule(BREAD_BAKERY, "bread|pandesal|buns?|tortillas?|pastry|baguette|pita|rolls?|croissant|bagel|brioche"
                    + "|sponge|biscuits?|crackers?|wrappers?"),
            rule(FRUITS_VEGETABLES, "onions?|garlic|tomato(es)?|potato(es)?|carrots?|cabbage|lettuce|spinach|broccoli"
                    + "|chil+i(es)?|capsicum|ginger|lemons?|limes?|bananas?|mango|apples?|pineapple|cucumber|eggplant"
                    + "|aubergine|mushrooms?|sprouts?|celery|scallions?|spring onions?|leek|pumpkin|squash|corn|peas?"
                    + "|avocado|kangkong|okra|radish|beet|kale|cassava|taro|calamansi|oranges?|grapes?|strawberr(y|ies)"
                    + "|berr(y|ies)|papaya|green beans?|string beans?|ampalaya|pechay|bok choy|zucchini|cauliflower"
                    + "|shallots?|lemongrass|pandan|tamarind|leaves|leaf"),
            rule(GRAINS_PANTRY, "rice|sugar|oil|oats|oatmeal|lentils?|beans?|nuts?|peanuts?|cashews?|almonds?|chocolate"
                    + "|cocoa|yeast|water|canned|tinned|coconut|raisins?|sesame|gelatin|tapioca|sago|evaporated"
                    + "|condensed|breadcrumbs?|panko"));

    private static Map.Entry<FoodGroup, Pattern> rule(FoodGroup group, String words) {
        return Map.entry(group, Pattern.compile("\\b(" + words + ")\\b"));
    }

    /** The food group of an ingredient name, for example "Garlic" gives Fruits & Vegetables. */
    public static FoodGroup of(String ingredientName) {
        String name = ingredientName == null ? "" : ingredientName.trim().toLowerCase(Locale.ROOT);
        if (VEGETABLE_PEPPER.matcher(name).find()) {
            return FRUITS_VEGETABLES;
        }
        return RULES.stream()
                .filter(rule -> rule.getValue().matcher(name).find())
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(OTHER);
    }

    /** Reads the name() saved in the database; an unknown or empty value becomes OTHER. */
    public static FoodGroup fromName(String name) {
        try {
            return name == null ? OTHER : valueOf(name.trim());
        } catch (IllegalArgumentException e) {
            return OTHER;
        }
    }
}
