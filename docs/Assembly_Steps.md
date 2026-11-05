# How to put Sections 4 to 7 into the Word file (Kirk, Day 2)
Use your copy `Design_Document_v0.3.docx`. Do these in order. Take your time. It is worth 25 points!

## 0. What you need (all are in the repo after the others push)
- `docs/sections/Section5_API_Integration.md`   (Nesley)
- `docs/sections/Section6_Concurrency.md`       (Yvonne)
- `docs/sections/Section7_Mining_and_Visualization.md`  (Ken)
- `docs/sections/Appendix_Shopping_List_and_Streak.md`  (Kirk, your own)
- `docs/sections/Section4_Update_meal_tables.md` and `docs/ERD_meal_tables.png`  (Ken)
- Screenshots from Kimberly, Nesley, Yvonne and Ken (see `docs/SCREENSHOT_LIST.md`).
Get them: in your Codespace run `git checkout project/milestone-2` and `git pull`.
Read each file with right-click, **Open Preview**. Check there are NO `[YOUR TURN]` left. If there are, ask the owner.

## 1. Paste a section
1. In Word, find the heading, for example **5. API Integration Design**. Click on the empty line under it.
2. In the Preview, select the text of the section (but not the "How to use this file" lines at the top), copy it.
3. In Word: Home tab, click the little arrow under **Paste**, choose **Keep Text Only** (the clipboard with an A).
4. Select what you pasted. Set the font to **Calibri**, size **11**. Set line spacing to **1.15**. Align **Left**.
5. Lines that start with `###` are sub-headings: select the line, remove the `###`, and give it the **Heading 2** style (Home tab, Styles).
6. Do the same for Sections 6 and 7.

## 2. Tables (caption ABOVE)
1. The `|` rows are tables. Select them, then Insert tab, Table, **Convert Text to Table**, separator = **Other: |**.
2. Delete the row that is only `---`.
3. Click inside the table. References tab, **Insert Caption**. Label = **Table**. Position = **Above selected item**. Press OK.
4. Type the description after the number, for example: `Endpoint used by MealDbClient`.
5. Numbers must CONTINUE from the last table in Week 9. Word does this for you if you use Insert Caption.

## 3. Pictures (caption BELOW)
1. Insert tab, **Pictures**, **This Device**, pick the screenshot.
2. Click the picture. References tab, **Insert Caption**. Label = **Figure**. Position = **Below selected item**.
3. Make the picture fit inside the margins (drag a corner).

## 4. Code (Consolas)
Select code lines. Font **Consolas**, size **9**. Keep each piece under 20 lines.

## 5. Section 4 update
Paste `Section4_Update_meal_tables.md` at the END of Section 4. Insert `ERD_meal_tables.png` as a Figure with a caption BELOW.

## 6. Front pages
- **Revision History**: fill the row **0.3 - Week 10 sections added** with the date and the authors (Nesley, Yvonne, Ken, Kirk).
- Click the Table of Contents, press **F9**, choose **Update entire table**.

## 7. Save
Save the Word file. Save a PDF copy: File, Save As, PDF. Day 3: upload both to the `docs` folder in the repo.
