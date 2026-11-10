-- Plate N Platter database: 5 tables, one primary key each.
-- Table names are plural; a primary key is named after one row (user_id, saved_recipe_id, ...);
-- a foreign key has the same name as the key it points to.
-- DatabaseConnector.createTables runs this file every time the app starts (IF NOT EXISTS makes that safe).

CREATE TABLE IF NOT EXISTS users (
    user_id              INTEGER PRIMARY KEY AUTOINCREMENT,
    username             TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    display_name         TEXT    NOT NULL,
    age                  INTEGER CHECK (age BETWEEN 1 AND 120),
    gender               TEXT,
    password_hash        TEXT    NOT NULL,
    salt                 TEXT    NOT NULL,
    security_question    TEXT    NOT NULL,
    security_answer_hash TEXT    NOT NULL
);

CREATE TABLE IF NOT EXISTS saved_recipes (
    saved_recipe_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id           INTEGER NOT NULL
        REFERENCES users(user_id) ON DELETE CASCADE,
    recipe_name       TEXT    NOT NULL COLLATE NOCASE,
    meal_type         TEXT    NOT NULL
        CHECK (meal_type IN ('BREAKFAST', 'MAIN_COURSE', 'DESSERT')),
    cuisine           TEXT    NOT NULL DEFAULT '',
    base_prep_minutes INTEGER NOT NULL DEFAULT 0 CHECK (base_prep_minutes >= 0),
    usual_calories    INTEGER NOT NULL DEFAULT 0 CHECK (usual_calories >= 0),
    mealdb_id         TEXT    NOT NULL DEFAULT '',
    UNIQUE (user_id, recipe_name)
);

CREATE TABLE IF NOT EXISTS ingredients (
    ingredient_id   INTEGER PRIMARY KEY AUTOINCREMENT,
    ingredient_name TEXT    NOT NULL UNIQUE COLLATE NOCASE,
    food_group      TEXT    NOT NULL DEFAULT 'OTHER'
);

CREATE TABLE IF NOT EXISTS ingredient_amounts (
    ingredient_amount_id INTEGER PRIMARY KEY AUTOINCREMENT,
    saved_recipe_id      INTEGER NOT NULL
        REFERENCES saved_recipes(saved_recipe_id) ON DELETE CASCADE,
    ingredient_id        INTEGER NOT NULL
        REFERENCES ingredients(ingredient_id),
    measure              TEXT    NOT NULL DEFAULT '',
    UNIQUE (saved_recipe_id, ingredient_id)
);

CREATE TABLE IF NOT EXISTS planned_meals (
    planned_meal_id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id         INTEGER NOT NULL
        REFERENCES users(user_id) ON DELETE CASCADE,
    saved_recipe_id INTEGER NOT NULL
        REFERENCES saved_recipes(saved_recipe_id),
    plan_date       TEXT    NOT NULL,                  -- yyyy-mm-dd
    meal_slot       TEXT    NOT NULL
        CHECK (meal_slot IN ('BREAKFAST', 'LUNCH', 'DINNER', 'SNACK')),
    prep_minutes    INTEGER NOT NULL CHECK (prep_minutes >= 0),
    calories        INTEGER NOT NULL DEFAULT 0 CHECK (calories >= 0),
    healthy_level   TEXT    NOT NULL
        CHECK (healthy_level IN ('HEALTHY', 'MODERATE', 'INDULGENT'))
);

CREATE INDEX IF NOT EXISTS idx_planned_meals_user_date
    ON planned_meals (user_id, plan_date);
