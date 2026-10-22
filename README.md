# cpe314-69167-capstoneteam-Meal_plannerGroup1
# Meal Planner – Program Flow

```text
START APP
  initialize database (create tables if they do not exist)
  show AUTH SCREEN

// ───────────── AUTH ─────────────
AUTH SCREEN: choose LOGIN or REGISTER

  LOGIN
    read username, password
    if any field is empty        -> show "Please fill in all fields"
    find user by username (PreparedStatement)
    hash typed password with stored salt
    if user not found OR hash does not match -> show "Incorrect username or password"
    else currentUser = user; go to HOME

  REGISTER
    read name, username, age, gender, password, confirmPassword
    if any field is empty                -> show error
    if age is not a valid number         -> show error
    if username already exists           -> show "Username taken"
    if password != confirmPassword       -> show "Passwords do not match"
    generate salt; hash password; save user to database
    show "Account created"; return to LOGIN

// ───────────── HOME (front page) ─────────────
HOME
  show greeting, live clock (updates every second), today's date
  show side menu: Calendar | Today's Meals | Meal Time | Shopping List
                  | Streak | Statistics | Undo | Logout
  wait for user click   (if no click, stay on HOME)

// ───────────── CALENDAR ─────────────
ON "Calendar"
  load currentUser's MealPlanEntries for the displayed month
  draw month grid (previous / next month buttons)
  for each date cell:
      for each meal on that date:
          background color = healthyLevel
              (Healthy = green, Moderate = yellow, Indulgent = red)
  highlight today's date
  on date click -> show that day's meals with edit / remove options
  show side options: Meal Time | Shopping List | Streak | Statistics

// ───────────── TODAY'S MEALS ─────────────
ON "Today's Meals"
  load today's entries for currentUser
  for slot in [Breakfast, Lunch, Dinner, Snack]:
      if slot has a planned dish:
          show dish photo (from API image URL), name, healthy level, difficulty
      else if slot == Snack: hide it   // snack is optional
      else show "Nothing planned" + "Add meal" button
  button "Mark as eaten" for each dish

// ───────────── MEAL TIME (adding dishes) ─────────────
ON "Meal Time"
  user picks a slot (Breakfast / Lunch / Dinner / Snack) and a date
  load dish options (saved recipes + API search results)
      -> API search runs on a BACKGROUND THREAD, show loading indicator
  show filters: healthy level, difficulty (Quick / Easy / Hard)

  ON dish click:
      add dish to the "Selected meals" panel on the side, grouped by slot (updates live)
      push action onto UNDO STACK

  ON "Add your own meal":
      form: name, meal type, healthy level, prep time, calories,
            ingredients, optional sub-recipes
      validate fields
      put recipe into PENDING QUEUE

  ON "Save plan":
      for each selected dish -> create MealPlanEntry (date, slot, recipe)
      background thread processes PENDING QUEUE in order,
          saving each recipe to the database (FIFO)
      refresh calendar

// ───────────── UNDO ─────────────
ON "Undo"
  if undo stack is empty -> show "Nothing to undo"
  else pop last action and reverse it (add, remove, or move), refresh screen

// ───────────── SHOPPING LIST ─────────────
ON "Shopping List"
  pick date range (default: this week)
  for each planned dish in range:
      recursively collect ingredients, including sub-recipes
      add to Map<ingredient, quantity> (merge duplicates)
  show list with checkboxes ("to buy")

// ───────────── STREAK ─────────────
ON "Streak"
  streak = number of consecutive days the user followed their plan
           (planned meals marked eaten)
  show current streak, best streak, progress bar to next milestone
  show achievements list (locked / unlocked)
  if streak reaches a milestone (e.g., 7, 14, 30 days):
      award badge; unlock "cheat day" reward
  show earned badges

// ───────────── STATISTICS ─────────────
ON "Statistics"
  load this week's eaten meals
  show BAR CHART: count of meals by healthy level (Healthy / Moderate / Indulgent)
  count frequency of each dish -> show top / favorite meals of the week
  average daily calories = total calories this week / number of days
  show all three

// ───────────── RECURSION (used by several screens) ─────────────
FUNCTION totalPrepTime(recipe, visited)
  if recipe in visited -> return 0        // prevents circular loops
  add recipe to visited
  total = recipe.prepTime
  for each subRecipe in recipe.components:
      total = total + totalPrepTime(subRecipe, visited)
  return total

FUNCTION difficulty(recipe)
  t = totalPrepTime(recipe, empty set)
  if t <= 15 and no sub-recipes -> "Quick"
  else if t <= 45              -> "Easy"
  else                         -> "Hard"

// ───────────── LOGOUT ─────────────
ON "Logout"
  currentUser = null; clear undo stack; return to AUTH SCREEN
END
```
