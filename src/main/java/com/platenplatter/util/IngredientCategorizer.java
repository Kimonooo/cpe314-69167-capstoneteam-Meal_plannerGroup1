package com.platenplatter.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Two jobs: (1) clean an ingredient name so "Eggs", "eggs " and "egg" become one item, and
 * (2) decide which shop aisle it belongs to.
 * The aisle rules are checked IN ORDER and the first match wins. That is why "chicken stock" is a
 * pantry item (the stock rule comes before the chicken rule) and "tomato paste" is a sauce.
 */
public final class IngredientCategorizer {
    private static final Map<String, String> SYNONYMS = Map.of(
            "eggs", "egg", "tomatoes", "tomato", "onions", "onion", "potatoes", "potato",
            "carrots", "carrot", "chilies", "chili", "chillies", "chili", "chilis", "chili", "lemons", "lemon");

    private static final class Rule {
        final Aisle aisle;
        final Pattern pattern;

        Rule(Aisle aisle, String words) {
            this.aisle = aisle;
            this.pattern = Pattern.compile("\\b(" + words + ")\\b");
        }
    }

    private static final List<Rule> RULES = new ArrayList<>();

    static {
        RULES.add(new Rule(Aisle.SAUCES, "sauce|vinegar|ketchup|mayo|mayonnaise|mustard|paste|syrup|honey|dressing|marinade|relish|jam|soy"));
        RULES.add(new Rule(Aisle.PANTRY, "stock|broth|bouillon|coconut milk|coconut cream|peanut butter|noodle|noodles|pasta|spaghetti"
                + "|macaroni|vermicelli|lasagne|lasagna|flour|baking powder|baking soda|cocoa powder|milk powder|cornstarch|corn starch"));
        RULES.add(new Rule(Aisle.SPICES, "powder|salt|peppercorn|peppercorns|black pepper|white pepper|ground pepper|pepper|paprika|cumin"
                + "|turmeric|cinnamon|oregano|thyme|rosemary|bay leaf|bay leaves|nutmeg|curry|vanilla|seasoning|spice|spices|cayenne"
                + "|saffron|cardamom|chili flakes|msg|cloves|clove|ground cloves"));
        RULES.add(new Rule(Aisle.DAIRY, "milk|cheese|butter|cream|yogurt|yoghurt|egg|eggs|mozzarella|parmesan|cheddar|margarine|ghee|custard"));
        RULES.add(new Rule(Aisle.MEAT, "chicken|beef|pork|bacon|ham|sausage|lamb|turkey|fish|salmon|tuna|shrimp|prawn|prawns|crab|squid"
                + "|bangus|milkfish|longganisa|tapa|tocino|liver|duck|mutton|anchovy|anchovies|sardine|sardines|mussel|mussels|clam"
                + "|clams|oyster|oysters|tilapia|cod|ribs|steak|mince|oxtail|tripe|pork belly|ham hock"));
        RULES.add(new Rule(Aisle.BAKERY, "bread|pandesal|bun|buns|tortilla|pastry|baguette|pita|roll|rolls|croissant|bagel|brioche|cake"
                + "|sponge|biscuit|biscuits|cracker|crackers|wrapper|wrappers"));
        RULES.add(new Rule(Aisle.PRODUCE, "onion|garlic|tomato|potato|carrot|cabbage|lettuce|spinach|broccoli|bell pepper|red pepper"
                + "|green pepper|chili|chilli|capsicum|ginger|lemon|lime|banana|mango|apple|pineapple|cucumber|eggplant|aubergine"
                + "|mushroom|mushrooms|sprout|sprouts|celery|coriander|cilantro|parsley|basil|mint|scallion|scallions|spring onion"
                + "|leek|pumpkin|squash|corn|pea|peas|avocado|kangkong|okra|radish|turnip|beet|kale|cassava|taro|gabi|calamansi"
                + "|orange|grape|grapes|strawberry|berry|berries|watermelon|papaya|jackfruit|green bean|green beans|string bean"
                + "|long bean|snake bean|ampalaya|bitter melon|sitaw|pechay|bok choy|zucchini|cauliflower|herb|herbs|shallot"
                + "|shallots|chives|dill|lemongrass|lemon grass|pandan|tamarind|leaves|leaf"));
        RULES.add(new Rule(Aisle.PANTRY, "rice|sugar|oil|oats|oatmeal|lentil|lentils|bean|beans|nut|nuts|peanut|peanuts|cashew|almond"
                + "|chocolate|cocoa|yeast|baking|water|canned|tinned|coconut|raisin|raisins|sesame|gelatin|tapioca|sago|macapuno"
                + "|kaong|nata|ube|langka|evaporated|condensed|breadcrumb|breadcrumbs|panko|cooking spray"));
    }

    private IngredientCategorizer() { }

    /** "  Eggs " becomes "egg". Returns an empty string for blank or null input. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        s = s.replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
        return SYNONYMS.getOrDefault(s, s);
    }

    /** "chicken breast" becomes "Chicken breast". */
    public static String display(String normalized) {
        if (normalized.isEmpty()) {
            return normalized;
        }
        return Character.toUpperCase(normalized.charAt(0)) + normalized.substring(1);
    }

    /** The shop aisle of an already normalized ingredient name (anything unknown goes to OTHER). */
    public static Aisle aisleFor(String normalized) {
        // "bell pepper" is a vegetable, but a bare "pepper" is the spice
        if (normalized.matches(".*\\b(bell|red|green|yellow|chili|chilli|hot) pepper\\b.*")) {
            return Aisle.PRODUCE;
        }
        for (Rule rule : RULES) {
            if (rule.pattern.matcher(normalized).find()) {
                return rule.aisle;
            }
        }
        return Aisle.OTHER;
    }
}
