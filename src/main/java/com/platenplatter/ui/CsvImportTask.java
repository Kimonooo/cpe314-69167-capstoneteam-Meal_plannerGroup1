package com.platenplatter.ui;

import com.platenplatter.util.CsvRecipeParser;
import com.platenplatter.util.PlanStore;
import java.io.File;
import java.util.List;
import java.util.stream.IntStream;
import javafx.concurrent.Task;

/**
 * Imports a meal-history CSV on a BACKGROUND THREAD. call() never touches the screen; it reports
 * through updateProgress and updateMessage every 50 rows, which the Import page binds to its bar.
 * A bad row is counted as skipped and the import goes on; Cancel stops it before the next row.
 */
public class CsvImportTask extends Task<Integer> {
    private final File file;
    private final PlanStore store;
    private volatile int imported = 0;
    private volatile int skipped = 0;

    public CsvImportTask(File file, PlanStore store) {
        this.file = file;
        this.store = store;
    }

    public int getImported() {
        return imported;
    }

    public int getSkipped() {
        return skipped;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Reading file...");
        List<String> lines = CsvRecipeParser.readLines(file.toPath());
        int total = lines.size() - 1;                      // line 0 is the header
        try {
            IntStream.rangeClosed(1, total)
                    .takeWhile(i -> !isCancelled())         // Cancel stops before the next row
                    .forEach(i -> importLine(lines.get(i), i, total));
        } catch (IllegalStateException e) {
            throw (Exception) e.getCause();                 // a real database problem stops the import
        }
        updateProgress(1, 1);
        return imported;
    }

    private void importLine(String line, int row, int total) {
        try {
            store.saveMeal(CsvRecipeParser.parse(line), List.of());
            imported++;
        } catch (IllegalArgumentException badRow) {
            skipped++;                                      // a bad row never stops the import
        } catch (Exception databaseProblem) {
            throw new IllegalStateException(databaseProblem);
        }
        if (row % 50 == 0 || row == total) {               // don't flood the UI thread
            updateProgress(row, total);
            updateMessage("Imported " + imported + " rows, skipped " + skipped);
        }
    }
}
