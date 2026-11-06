# Kimberly's Guide - Team Leader

**Your role:** Team Leader.   **Your zip:** Day0_Kimberly_BASE.zip   **Your branch:** project/milestone-2 (you create it)

**The big picture in kid words:** You start the team's work area for this week (a new branch), put Claude's first pieces into it, and then everybody else joins. During the 3 days you check and merge teammates' work, connect the new tabs into the app (only YOU edit App.java), and send the final submission. You are the traffic officer: nobody starts until you say go.

**Your points:** keeping the group on track, the Stack/Queue/recursion re-demo (6), Known Issues + requirement map (4), the final PR.


### Where you work: your LAPTOP (not the Codespace), because your working app is there.


### WORDS YOU WILL SEE (read this once)

- **Repository (repo)** = the shared folder for our whole project, kept on GitHub.
- **Codespace** = a computer in the internet cloud that opens VS Code in your browser. We type commands there.
- **Terminal** = the black/dark box at the bottom where you type commands. You type a line, then press Enter.
- **Branch** = your own copy of the project to work on, so you do not break everyone else's work.
- **Commit** = saving a snapshot of your work with a short message (like 'Save game').
- **Push** = sending your commits up to GitHub so teammates can see them.
- **Pull** = bringing teammates' new work down to your computer.
- **Pull Request (PR)** = a message on GitHub saying 'please add my work to the team branch'.
- **Zip file** = a folder squished into one file. We unzip it to get the folder back.
- **Explorer** = the list of files on the LEFT side of VS Code.


## PART 1 - DAY 0: CREATE THE BRANCH AND INSTALL THE BASE


### 1A. Open Command Prompt on your laptop

1. Press the Windows key, type  cmd , press Enter. A black window opens.
2. Go to the project folder. Type and press Enter:
```
cd C:\Projects\cpe314-69167-capstoneteam-Meal_plannerGroup1
```
   - You should see: The line now ends with the folder name.

### 1B. Make the new branch from your Milestone 1 branch

We branch from project/milestone-1 (NOT main) because main does not have our code yet.

1. Type these one at a time, pressing Enter after each:
```
git checkout project/milestone-1
git pull
git checkout -b project/milestone-2
```
   - You should see: Switched to a new branch 'project/milestone-2'.

### 1C. Put the BASE zip into the project

1. Download **Day0_Kimberly_BASE.zip** from the chat. Move it into the project folder  C:\Projects\cpe314-69167-capstoneteam-Meal_plannerGroup1  (drag it there in File Explorer).
2. Back in Command Prompt, type:
```
tar -xf Day0_Kimberly_BASE.zip
```
   - You should see: No message at all = success. New folders appear: src, sample_data, docs.
3. Delete the zip so it is not saved into the project:
```
del Day0_Kimberly_BASE.zip
```

### 1D. Add one dependency to pom.xml

1. Open the project folder in VS Code (or Notepad). Open the file **pom.xml**.
2. Find the word  <dependencies>  (with the angle brackets). Everything between  <dependencies>  and  </dependencies>  is a list of tools the app uses.
3. Open **pom_snippet.xml** (it came in the zip). Copy the block that starts with  <dependency>  and ends with  </dependency>  (the Gson one).
4. Paste it inside pom.xml, just before the line  </dependencies>. Save the file (Ctrl+S).
5. Also check the Java version in pom.xml: search for  release  or  maven.compiler. It must be **11 or higher** (17 is fine). If it says 8, change it to 17.

### 1E. Test it

1. In Command Prompt type:
```
mvn test
```
   - You should see: BUILD SUCCESS. About 12 new tests plus your Milestone 1 tests. First time it may download things; wait.
   - WARNING: If BUILD FAILURE: take a screenshot and paste the red part to Claude. Do not push until it is green.

### 1F. Save and send

1. Type these one at a time:
```
git add .
git commit -m "chore: add Milestone 2 base (API client, CSV import, analyzer, chart)"
git push -u origin project/milestone-2
```
   - You should see: Text ending with 'Branch project/milestone-2 set up to track ...'.
2. Check on github.com: open the repo, click the branch dropdown (it says main), and confirm you can see **project/milestone-2**.
3. Write in the group chat:  "BASE is merged. Everyone: open your Codespace, run  git fetch --all , then  git checkout project/milestone-2 , then  git pull , then follow your own guide."

## PART 2 - DAY 1: MERGE YOUR TEAMMATES' PRs

Each teammate will say 'my PR is ready'. You check it and merge it. Merging means putting their work into project/milestone-2.

1. On github.com open the repo and click the **Pull requests** tab. Click a teammate's PR.
2. Check the top: it must say  wants to merge ... into **project/milestone-2**  (NOT main). If it says main, click **Edit** next to the title and change the base.
3. Click the **Files changed** tab and look quickly: are there strange files like .zip or huge files? If yes, ask them to remove it.
4. Back on the **Conversation** tab, click the green **Merge pull request** button, then **Confirm merge**.
   - You should see: A purple 'Merged' badge.
5. After merging, tell the group "PR merged". Anyone who needs the newest code runs these in their terminal:  git fetch --all  then  git checkout project/milestone-2  then  git pull.
   - WARNING: If GitHub says 'This branch has conflicts' do not panic. Ask Claude with a screenshot, or fix with the teammate on a call. Never click anything random.
6. Update the file docs/REQUIREMENT_TO_CODE_MAP.md as classes arrive (replace [FILL] with real file names).

## PART 3 - DAY 2: CONNECT THE TABS (only YOU edit App.java)

1. Wait until Nesley's, Yvonne's and Ken's PRs are all merged. On your laptop run:
```
git checkout project/milestone-2
git pull
```
2. Ask Ken how the app gets the database Connection (he writes it in the group chat).
3. Open  docs/AppWiring.md. It has about 10 lines of code to paste into **App.java** where your existing tabs (Sign up, Calendar) are created. Replace the word  conn  with the way Ken described.
   - WARNING: I have not seen your App.java. If you get stuck, paste App.java and Database.java into the chat with Claude and ask for the exact edit.
4. Run the app with  mvn javafx:run . You must see 5 tabs: Sign up, Calendar, Recipes, Import, Analytics.
5. Do a fast test: Recipes > search chicken > Add to plan. Import tab > import sample_data/meal_history_small.csv. Analytics > Refresh. You should see bars.
6. Commit and push this on a branch called  feature/integration  (same Part 4 steps below) and merge it yourself into project/milestone-2 after Venice checks.

## PART 4 - SAVE AND SEND ANY OF YOUR OWN CHANGES

```
mvn test
git add .
git commit -m "feat: wire Recipes, Import and Analytics tabs"
git push
```

## DAY 3 - THE FINAL SUBMISSION

1. Make sure everything is merged into project/milestone-2 and  mvn test  is green.
2. Finish docs/REQUIREMENT_TO_CODE_MAP.md and docs/KNOWN_ISSUES.md (Venice's).
3. On github.com click **Pull requests > New pull request**. Set **base: main** and **compare: project/milestone-2**.
4. Title it EXACTLY:  **Milestone 2 - Team NN**  (use the same NN as your Milestone 1 PR; use the long dash if Milestone 1 title used it).
5. Click **Create pull request** BEFORE the deadline. Then DO NOT MERGE IT. The instructor merges it after grading.
6. Remind everyone to fill in the peer evaluation sheet.

### Your explain-it practice

- Point to CookingQueue (Queue), UndoStack (Stack), MealPlanAnalyzer (Map) and RecipeCalculator (recursion).
- Trace RecipeCalculator by hand for a small recipe: write the steps on paper so you can explain the base case (the stopping rule).


## IF SOMETHING GOES WRONG (read before you panic)

- **The terminal prints red text and you do not understand it:** take a screenshot, send it to the group chat. Do NOT keep guessing commands.
- **You typed a command wrong:** nothing bad happens. Press the Up arrow key to bring back the last command, fix it, press Enter.
- **The prompt shows the wrong branch:** run  git branch  to see all branches. The one with a star is the one you are on. Switch with  git checkout branch-name.
- **'merge conflict' message:** this means two people changed the same file. Do not touch anything. Tell Kimberly. She will fix it with you.
- **You deleted a file by accident:** run  git checkout -- filename  to bring it back (before you commit).
- **The Codespace says it is stopped or slow:** refresh the browser page and click your codespace again. Your files are still there.
- **You are stuck for more than 10 minutes:** ask. Asking early is smart, not embarrassing.

