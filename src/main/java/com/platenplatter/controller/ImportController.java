package com.platenplatter.controller;

import com.platenplatter.api.MealDbClient;
import com.platenplatter.db.PlanStore;
import com.platenplatter.service.MealImporter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;

/**
 * The controller of the Import page: it runs MealImporter on the user's PlanStore and turns the
 * result into the sentences the page shows. CsvImportTask calls importFile on a background thread.
 */
public class ImportController {
    private final PlanStore store;

    public ImportController(PlanStore store) {
        this.store = store;
    }

    public MealImporter.Result importFile(File file, MealImporter.Progress progress)
            throws IOException, SQLException {
        return MealImporter.importFile(file.toPath(), store, progress);
    }

    /** "Done! 2 meals imported. 4 bad rows skipped. 1 was already in your plan." */
    public static String summary(MealImporter.Result result) {
        StringBuilder text = new StringBuilder("Done! ").append(count(result.imported(), "meal"))
                .append(" imported.");
        if (result.badRows() > 0) {
            text.append(' ').append(count(result.badRows(), "bad row")).append(" skipped.");
        }
        if (result.duplicates() > 0) {
            text.append(' ').append(count(result.duplicates(), "meal"))
                    .append(result.duplicates() == 1 ? " was"
                            : " were").append(" already in your plan.");
        }
        return text.toString();
    }

    /** The sentence for a failed import: a file problem, or anything else in plain words. */
    public static String failure(Throwable error) {
        if (error instanceof IOException && !MealDbClient.isOffline(error)) {
            return "The file could not be read. Check that it is a .csv file that is not open in "
                    + "another program, then try again.";
        }
        if (error instanceof SQLException) {
            return "The import could not be saved, so nothing was added. Please try again.";
        }
        return UserMessages.of(error);
    }

    private static String count(int n, String word) {
        return String.format(Locale.ENGLISH, "%,d", n) + " " + word + (n == 1 ? "" : "s");
    }
}
