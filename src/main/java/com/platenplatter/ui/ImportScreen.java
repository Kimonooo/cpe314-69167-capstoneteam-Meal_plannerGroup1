package com.platenplatter.ui;

import com.platenplatter.util.RowSink;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;

/**
 * The "Import" tab.
 * What it does: loads a big meal-history CSV into the database WITHOUT freezing the window.
 * The import runs in CsvImportTask on a BACKGROUND THREAD. The progress bar, the percent and
 * the status line are bound to that task, so you can click other tabs while it runs.
 * Controls that work: Choose CSV, drag-and-drop, Import, Cancel.
 */
public class ImportScreen {
    private final RowSink sink;
    private Runnable onImportFinished = () -> { };

    private final VBox root = new VBox(14);
    /** The page scrolls when the window is shorter than it, so nothing at the bottom is cut off. */
    private final ScrollPane scroller = new ScrollPane(root);
    private final VBox dropZone = new VBox(10);
    private final Button chooseButton = new Button("Choose CSV...");
    private final Label fileLabel = new Label("No file chosen yet");
    private final Button importButton = new Button("Import");
    private final Button cancelButton = new Button("Cancel");
    private final ProgressBar bar = new ProgressBar(0);
    private final Label percentLabel = new Label();
    private final Label statusLabel = new Label();
    private final HBox resultsRow = new HBox(14);
    private final Label importedValue = new Label("0");
    private final Label skippedValue = new Label("0");
    private final Label timeValue = new Label("0 s");

    private File chosenFile;
    private CsvImportTask currentTask;
    private long startNanos;

    public ImportScreen(RowSink sink) {
        this.sink = sink;

        root.setPadding(new Insets(22));
        root.getChildren().addAll(buildHeader(), buildPickCard(), buildImportCard(), resultsRow, buildFormatCard());
        Theme.apply(root);
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        Theme.status(statusLabel, "Choose a CSV file to begin.", Theme.Kind.INFO);
        resultsRow.setVisible(false);
        resultsRow.setManaged(false);
        wireEvents();
    }

    public Parent getView() {
        return scroller;
    }

    /** Called when an import ends (finished, failed or cancelled), so other tabs can refresh. */
    public void setOnImportFinished(Runnable onImportFinished) {
        this.onImportFinished = onImportFinished == null ? () -> { } : onImportFinished;
    }

    // ------------------------------------------------------------------ building the screen

    private VBox buildHeader() {
        Label title = new Label("Import Meal History");
        title.getStyleClass().add("pnp-title");
        Label subtitle = new Label("Load thousands of past meals from a CSV file. It runs in the background, so you can keep using the app.");
        subtitle.getStyleClass().add("pnp-subtitle");
        subtitle.setWrapText(true);
        return new VBox(2, title, subtitle);
    }

    private VBox buildPickCard() {
        Label step = new Label("Step 1: Pick your file");
        step.getStyleClass().add("pnp-section");

        Label dropText = new Label("Drop a .csv file here");
        dropText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label or = new Label("or");
        or.getStyleClass().add("pnp-muted");
        chooseButton.setId("chooseButton");
        chooseButton.getStyleClass().add("pnp-button-secondary");
        fileLabel.setId("fileLabel");
        fileLabel.getStyleClass().add("pnp-muted");

        dropZone.getChildren().addAll(dropText, or, chooseButton, fileLabel);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.getStyleClass().add("pnp-drop");

        VBox card = new VBox(10, step, dropZone);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private VBox buildImportCard() {
        Label step = new Label("Step 2: Import it");
        step.getStyleClass().add("pnp-section");

        importButton.setId("importButton");
        importButton.getStyleClass().add("pnp-button");
        importButton.setDisable(true);
        cancelButton.setId("cancelButton");
        cancelButton.getStyleClass().add("pnp-button-secondary");
        cancelButton.setDisable(true);
        HBox buttons = new HBox(10, importButton, cancelButton);

        bar.setId("importBar");
        bar.getStyleClass().add("pnp-progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        percentLabel.setMinWidth(44);
        percentLabel.setStyle("-fx-font-weight: bold;");
        percentLabel.textProperty().bind(Bindings.createStringBinding(() -> {
            double p = bar.getProgress();
            return p < 0 ? "" : Math.round(p * 100) + "%";
        }, bar.progressProperty()));
        HBox progressRow = new HBox(10, bar, percentLabel);
        progressRow.setAlignment(Pos.CENTER_LEFT);

        statusLabel.setId("importStatus");
        VBox card = new VBox(10, step, buttons, progressRow, statusLabel);
        card.getStyleClass().add("pnp-card");

        resultsRow.getChildren().addAll(statCard("ROWS IMPORTED", importedValue),
                statCard("BAD ROWS SKIPPED", skippedValue), statCard("TIME TAKEN", timeValue));
        return card;
    }

    private VBox statCard(String caption, Label valueLabel) {
        valueLabel.getStyleClass().add("pnp-stat-value");
        Label cap = new Label(caption);
        cap.getStyleClass().add("pnp-stat-label");
        VBox card = new VBox(2, cap, valueLabel);
        card.getStyleClass().add("pnp-card");
        card.setMaxWidth(Double.MAX_VALUE);
        card.setPrefWidth(10);
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private VBox buildFormatCard() {
        Label title = new Label("What the CSV must look like");
        title.getStyleClass().add("pnp-section");
        Label example = new Label("date,recipe_name,category,prep_minutes,meal_slot,calories,healthy_level\n"
                + "2026-11-03,Chicken Adobo,MainCourse,45,Dinner,620,Healthy\n"
                + "2026-11-04,Leche Flan,Dessert,50,Snack,350,Indulgent\n"
                + "2026-11-05,Tapsilog,Breakfast,20");
        example.getStyleClass().add("pnp-mono");
        Label note = new Label("The last three columns are optional, so old 4-column files still work. "
                + "Without them a meal gets the slot of its type (Breakfast, Snack for desserts, otherwise Dinner) "
                + "and its healthy level is worked out from the calories. The meals are added to YOUR plan. "
                + "Rows with a wrong date, a missing name, a bad number or an unknown slot or level are skipped "
                + "and counted. They never stop the import.");
        note.getStyleClass().add("pnp-muted");
        note.setWrapText(true);
        VBox card = new VBox(8, title, example, note);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void wireEvents() {
        chooseButton.setOnAction(e -> chooseFile());
        importButton.setOnAction(e -> startImport());
        cancelButton.setOnAction(e -> {
            if (currentTask != null) {
                currentTask.cancel();
            }
        });

        dropZone.setOnDragOver(e -> {
            if (currentTask == null && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                Theme.addClass(dropZone, "drag-over");
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drag-over"));
        dropZone.setOnDragDropped(e -> {
            List<File> files = e.getDragboard().getFiles();
            boolean ok = false;
            if (currentTask == null && !files.isEmpty()) {
                File file = files.get(0);
                if (file.getName().toLowerCase().endsWith(".csv")) {
                    pick(file);
                    ok = true;
                } else {
                    Theme.status(statusLabel, "Please drop a .csv file.", Theme.Kind.ERROR);
                }
            }
            e.setDropCompleted(ok);
            e.consume();
        });
    }

    /** Stops an import that is still running (used when the user logs out). Safe to call any time. */
    public void cancelImport() {
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    /** Picks a file in code (used by tests and previews), exactly as if the user had chosen it. */
    public void loadFile(File file) {
        pick(file);
    }

    // ------------------------------------------------------------------ actions

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose meal-history CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File sampleFolder = new File("sample_data");
        if (sampleFolder.isDirectory()) {
            chooser.setInitialDirectory(sampleFolder);
        }
        File file = chooser.showOpenDialog(chooseButton.getScene().getWindow());
        if (file != null) {
            pick(file);
        }
    }

    private void pick(File file) {
        chosenFile = file;
        fileLabel.setText(file.getName() + "   (" + humanSize(file.length()) + ")");
        importButton.setDisable(false);
        resultsRow.setVisible(false);
        resultsRow.setManaged(false);
        bar.setProgress(0);
        Theme.status(statusLabel, "Ready. Press Import.", Theme.Kind.INFO);
    }

    private void startImport() {
        if (chosenFile == null) {
            Theme.status(statusLabel, "Choose a CSV file first.", Theme.Kind.ERROR);
            return;
        }
        CsvImportTask task = new CsvImportTask(chosenFile, sink);
        currentTask = task;
        startNanos = System.nanoTime();

        chooseButton.setDisable(true);
        importButton.setDisable(true);
        cancelButton.setDisable(false);
        resultsRow.setVisible(false);
        resultsRow.setManaged(false);
        bar.progressProperty().bind(task.progressProperty());
        statusLabel.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> finish(task, "Done! The import finished.", 1.0, Theme.Kind.OK, true));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            finish(task, "Import failed: " + (ex == null ? "unknown error" : ex.getMessage()), 0, Theme.Kind.ERROR, false);
        });
        task.setOnCancelled(e -> finish(task, "Import cancelled.", 0, Theme.Kind.INFO, true));

        Thread thread = new Thread(task); // BACKGROUND THREAD: the UI stays responsive
        thread.setDaemon(true);
        thread.start();
    }

    private void finish(CsvImportTask task, String message, double progress, Theme.Kind kind, boolean showNumbers) {
        statusLabel.textProperty().unbind();
        bar.progressProperty().unbind();
        Theme.status(statusLabel, message, kind);
        bar.setProgress(progress);

        if (showNumbers) {
            Integer imported = task.getValue();
            importedValue.setText(String.format("%,d", imported == null ? 0 : imported));
            skippedValue.setText(String.format("%,d", task.getSkipped()));
            timeValue.setText(String.format("%.1f s", (System.nanoTime() - startNanos) / 1_000_000_000.0));
            resultsRow.setVisible(true);
            resultsRow.setManaged(true);
        }
        chooseButton.setDisable(false);
        importButton.setDisable(chosenFile == null);
        cancelButton.setDisable(true);
        currentTask = null;
        onImportFinished.run();
    }

    private static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024L * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
