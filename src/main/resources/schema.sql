CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    username TEXT NOT NULL UNIQUE,
    age INTEGER,
    gender TEXT,
    password_hash TEXT NOT NULL,
    salt TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS recipes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER,
    name TEXT NOT NULL,
    meal_type TEXT NOT NULL,
    cuisine TEXT,
    healthy_level TEXT,
    prep_minutes INTEGER NOT NULL DEFAULT 0,
    calories INTEGER NOT NULL DEFAULT 0,
    external_id TEXT,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS recipe_components (
    parent_recipe_id INTEGER NOT NULL,
    sub_recipe_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (parent_recipe_id, sub_recipe_id),
    FOREIGN KEY (parent_recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (sub_recipe_id) REFERENCES recipes(id)
);

CREATE TABLE IF NOT EXISTS ingredients (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS recipe_ingredients (
    recipe_id INTEGER NOT NULL,
    ingredient_id INTEGER NOT NULL,
    measure TEXT,
    PRIMARY KEY (recipe_id, ingredient_id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id),
    FOREIGN KEY (ingredient_id) REFERENCES ingredients(id)
);

CREATE TABLE IF NOT EXISTS meal_plan_entries (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    recipe_id INTEGER NOT NULL,
    plan_date TEXT NOT NULL,
    meal_slot TEXT NOT NULL,
    eaten INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (recipe_id) REFERENCES recipes(id)
);

-- The table every tab of the app reads and writes (MealPlanHistoryDao creates the same table,
-- and adds the newer columns to a database file that was made by an older build).
--   user_id       whose meal it is (NULL only for rows saved before accounts existed)
--   meal_slot     BREAKFAST, LUNCH, DINNER or SNACK
--   calories      kcal of one serving, 0 = not known
--   healthy_level HEALTHY, MODERATE or INDULGENT
-- The prep-time tier (Quick, Average, Slow) is NOT a column: it is worked out from prep_minutes.
CREATE TABLE IF NOT EXISTS meal_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER REFERENCES users(id),
    plan_date TEXT NOT NULL,
    meal_slot TEXT NOT NULL DEFAULT 'DINNER' CHECK (meal_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    recipe_name TEXT NOT NULL,
    category TEXT NOT NULL,
    prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0),
    calories INTEGER NOT NULL DEFAULT 0 CHECK (calories >= 0),
    healthy_level TEXT NOT NULL DEFAULT 'MODERATE' CHECK (healthy_level IN ('HEALTHY', 'MODERATE', 'INDULGENT'))
);

CREATE TABLE IF NOT EXISTS meal_ingredient (
    recipe_name TEXT NOT NULL COLLATE NOCASE,
    ingredient TEXT NOT NULL,
    PRIMARY KEY (recipe_name, ingredient)
);