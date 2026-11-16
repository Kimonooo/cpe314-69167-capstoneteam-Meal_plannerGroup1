package com.platenplatter.ui;

import com.platenplatter.controller.ImportController;
import com.platenplatter.service.MealImporter;
import java.io.File;
import javafx.concurrent.Task;

/**
 * Imports a meal-history CSV on a BACKGROUND THREAD. call() never touches the screen: it runs
 * the import through ImportController and reports with updateProgress and updateMessage (every
 * 50 rows), which the Import page binds to its bar. Cancel is checked after every row; then
 * the whole import is rolled back, so a cancelled import adds nothing.
 */
public class CsvImportTask extends Task<MealImporter.Result> {
    private final File file;
    private final ImportController controller;

    public CsvImportTask(File file, ImportController controller) {
        this.file = file;
        this.controller = controller;
    }

    @Override
    protected MealImporter.Result call() throws Exception {
        return controller.importFile(file, new MealImporter.Progress() {
            @Override
            public void update(int done, int total, String message) {
                updateProgress(done, total);            // safe from any thread: JavaFX passes it on
                updateMessage(message);
            }

            @Override
            public boolean isCancelled() {
                return CsvImportTask.this.isCancelled();
            }
        });
    }
}
