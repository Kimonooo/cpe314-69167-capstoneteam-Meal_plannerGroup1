# Section 4 update - the new tables   (author: Ken, pasted by Kirk)
Paste this at the END of Section 4 (Data Model & Database Design). Put the picture `ERD_meal_tables.png` under it as a Figure (caption BELOW).

Milestone 2 adds two tables.

- `meal_history` stores every planned meal: the ones added from the Recipes tab and the ones imported from a CSV file. The Meal Plan calendar, the Today tab, the streak and the Analytics tab all read this table.
- `meal_ingredient` stores the ingredient list of a recipe. The Shopping List tab reads it. One recipe has many ingredients, so the relationship is one-to-many (1:M). The two tables are matched by `recipe_name` (ignoring capital letters). There is no foreign key on purpose: a big CSV import must be able to add meals whose ingredients are not known yet.

Table N: Columns of meal_history

| Column | Type | Rules |
|---|---|---|
| id | INTEGER | Primary key, AUTOINCREMENT |
| plan_date | TEXT | NOT NULL, written as yyyy-MM-dd (an index makes date searches fast) |
| recipe_name | TEXT | NOT NULL |
| category | TEXT | NOT NULL (Breakfast, MainCourse or Dessert) |
| prep_minutes | INTEGER | NOT NULL, CHECK (prep_minutes >= 0) |

Table N: Columns of meal_ingredient

| Column | Type | Rules |
|---|---|---|
| recipe_name | TEXT | NOT NULL, COLLATE NOCASE, part of the primary key |
| ingredient | TEXT | NOT NULL, part of the primary key (so the same ingredient is never saved twice for one recipe) |

```
CREATE TABLE IF NOT EXISTS meal_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    plan_date TEXT NOT NULL,
    recipe_name TEXT NOT NULL,
    category TEXT NOT NULL,
    prep_minutes INTEGER NOT NULL CHECK (prep_minutes >= 0)
);
CREATE TABLE IF NOT EXISTS meal_ingredient (
    recipe_name TEXT NOT NULL COLLATE NOCASE,
    ingredient TEXT NOT NULL,
    PRIMARY KEY (recipe_name, ingredient)
);
CREATE INDEX IF NOT EXISTS idx_meal_history_date ON meal_history (plan_date);
```
Figure N: The two new tables and their 1:M relationship    <- ERD_meal_tables.png, caption goes BELOW
