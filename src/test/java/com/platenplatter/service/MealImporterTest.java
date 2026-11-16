package com.platenplatter.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.platenplatter.db.InMemoryPlanStore;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import org.junit.jupiter.api.Test;

/** The import without JavaFX: counts, blank lines, duplicates, Cancel and progress. */
class MealImporterTest {
    private static final Path BAD_ROWS = Path.of("sample_data", "meal_history_bad_rows.csv");
    private static final Path SMALL = Path.of("sample_data", "meal_history_small.csv");

    /** Records every progress message and can press Cancel after a number of updates. */
    private static final class Recorder implements MealImporter.Progress {
        private final List<String> messages = new ArrayList<>();
        private final int cancelAfterUpdates;

        Recorder(int cancelAfterUpdates) {
            this.cancelAfterUpdates = cancelAfterUpdates;
        }

        @Override
        public void update(int done, int total, String message) {
            messages.add(message);
        }

        @Override
        public boolean isCancelled() {
            return messages.size() >= cancelAfterUpdates;
        }
    }

    @Test
    void aBlankLineIsNotCountedAsABadRow() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        MealImporter.Result result = MealImporter.importFile(BAD_ROWS, store,
                new Recorder(Integer.MAX_VALUE));
        assertEquals(2, result.imported());
        assertEquals(4, result.badRows()); // the teacher's run showed 5: the blank line
        assertEquals(1, result.blankLines());
        assertEquals(2, store.allMeals().size());
    }

    @Test
    void importingTheSameFileTwiceAddsNothingTheSecondTime() throws Exception {
        InMemoryPlanStore store = new InMemoryPlanStore();
        MealImporter.Result first = MealImporter.importFile(SMALL, store,
                new Recorder(Integer.MAX_VALUE));
        MealImporter.Result second = MealImporter.importFile(SMALL, store,
                new Recorder(Integer.MAX_VALUE));
        assertEquals(0, second.imported());
        assertEquals(first.imported(), second.duplicates());
        assertEquals(first.imported(), store.allMeals().size());
    }

    @Test
    void cancelKeepsNothing() {
        InMemoryPlanStore store = new InMemoryPlanStore();
        assertThrows(CancellationException.class,
                () -> MealImporter.importFile(SMALL, store, new Recorder(1)));   // cancel at once
        assertTrue(store.allMeals().isEmpty());
    }

    @Test
    void aFileWithOnlyAHeaderImportsNothing() throws IOException, SQLException {
        Path file = Files.createTempFile("header_only", ".csv");
        try {
            Files.writeString(file, "date,recipe_name,category,prep_minutes\n\n");
            MealImporter.Result result = MealImporter.importFile(file, new InMemoryPlanStore(),
                    new Recorder(Integer.MAX_VALUE));
            assertEquals(0, result.rows());
            assertEquals(1, result.blankLines());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    @Test
    void progressEndsWithDone() throws Exception {
        Recorder recorder = new Recorder(Integer.MAX_VALUE);
        MealImporter.importFile(SMALL, new InMemoryPlanStore(), recorder);
        assertEquals("Reading the file...", recorder.messages.get(0));
        assertEquals("Done", recorder.messages.get(recorder.messages.size() - 1));
    }
}
