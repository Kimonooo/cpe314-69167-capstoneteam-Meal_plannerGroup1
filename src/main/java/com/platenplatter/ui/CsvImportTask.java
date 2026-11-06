package com.platenplatter.ui;

import com.platenplatter.util.CsvRecipeParser;
import com.platenplatter.util.RowSink;
import javafx.concurrent.Task;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * BACKGROUND THREAD: reads a big CSV and sends each valid row to a RowSink.
 * Run it with:  new Thread(task).start();   (never call() on the UI thread)
 * Bind a ProgressBar:  bar.progressProperty().bind(task.progressProperty());
 * Result (task.getValue()) = number of rows imported; getSkipped() = bad rows.
 */
public class CsvImportTask extends Task<Integer> {
    private final File file;
    private final RowSink sink;
    private volatile int skipped = 0;

    public CsvImportTask(File file, RowSink sink) {
        this.file = file;
        this.sink = sink;
    }

    public int getSkipped() {
        return skipped;
    }

    @Override
    protected Integer call() throws Exception {
        updateMessage("Reading file...");
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        int total = lines.size() - 1; // line 0 is the header
        if (total <= 0) {
            updateProgress(1, 1);
            return 0;
        }
        int imported = 0;
        for (int i = 1; i < lines.size(); i++) {
            if (isCancelled()) {
                break;
            }
            try {
                sink.accept(CsvRecipeParser.parseLine(lines.get(i)));
                imported++;
            } catch (IllegalArgumentException badRow) {
                skipped++;
            }
            if (i % 50 == 0 || i == lines.size() - 1) { // don't flood the UI thread
                updateProgress(i, total);
                updateMessage("Imported " + imported + " rows, skipped " + skipped);
            }
        }
        return imported;
    }
}
