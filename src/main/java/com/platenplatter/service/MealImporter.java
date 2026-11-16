package com.platenplatter.service;

import com.platenplatter.db.PlanStore;
import com.platenplatter.db.PlanStore.BatchResult;
import com.platenplatter.model.PlannedMeal;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Imports a meal-history CSV file, with no JavaFX in it, so it can be tested on its own:
 *   1. read the lines (CsvRecipeParser.readLines: UTF-8 or Excel's Windows-1252);
 *   2. skip the header and every blank line (a blank line is not a row, so it is not "bad");
 *   3. parse each row; a bad row is counted and the import goes on;
 *   4. save all good rows in ONE transaction (PlanStore.saveMeals): a meal already planned is
 *      counted as a duplicate, and Cancel rolls everything back.
 * CsvImportTask runs this on a background thread and passes progress to the progress bar.
 */
public final class MealImporter {
    /** Progress is reported every this many rows, so the JavaFX thread is not flooded. */
    public static final int REPORT_EVERY = 50;

    /** How the import ended. Blank lines are left out of every count. */
    public record Result(int imported, int duplicates, int badRows, int blankLines) {
        public int rows() {
            return imported + duplicates + badRows;
        }
    }

    /** Where the importer reports to (the Task), and how it hears about Cancel. */
    public interface Progress {
        void update(int done, int total, String message);

        boolean isCancelled();
    }

    private MealImporter() { }

    public static Result importFile(Path file, PlanStore store, Progress progress)
            throws IOException, SQLException {
        progress.update(0, 1, "Reading the file...");
        List<String> lines = CsvRecipeParser.readLines(file);
        List<PlannedMeal> good = new ArrayList<>();
        int bad = 0;
        int blank = 0;
        // line 0: header
        for (String line : lines.subList(Math.min(1, lines.size()), lines.size())) {
            if (line.isBlank()) {
                blank++;
                continue;
            }
            try {
                good.add(CsvRecipeParser.parse(line));
            } catch (IllegalArgumentException badRow) {
                bad++;                                   // a bad row never stops the import
            }
        }
        int total = good.size();
        int badRows = bad;
        BatchResult saved = store.saveMeals(good, done -> {
            if (done % REPORT_EVERY == 0 || done == total) {
                progress.update(done, total, "Saving meal " + String.format(Locale.ENGLISH, "%,d",
                        done) + " of "
                        + String.format(Locale.ENGLISH, "%,d", total) + " (" + badRows
                                + " bad rows skipped)");
            }
            return !progress.isCancelled();              // false: the store rolls everything back
        });
        progress.update(1, 1, "Done");
        return new Result(saved.saved(), saved.duplicates(), bad, blank);
    }
}
