package com.platenplatter.ui;

import com.platenplatter.util.PlanStore;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/**
 * The Import page: pick or drop a CSV file and import it in the background with CsvImportTask.
 * The window stays responsive: the progress bar and the message follow the Task.
 * A colored indicator in the drop zone always says where the file is: not chosen yet,
 * uploaded and ready, importing, imported, cancelled or failed.
 */
public class ImportScreen {
    private final PlanStore store;
    private Runnable onImportFinished = () -> { };
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final VBox dropZone = new VBox(10);
    private final Button chooseButton = new Button("Choose CSV...");
    private final Label fileLabel = new Label("No file chosen yet");
    private final Label uploadIndicator = new Label();
    private final Button importButton = new Button("Import");
    private final Button cancelButton = new Button("Cancel");
    private final ProgressBar bar = new ProgressBar(0);
    private final Label statusLabel = new Label();
    private final HBox resultsRow = new HBox(14);
    private final Label importedValue = new Label("0");
    private final Label skippedValue = new Label("0");
    private final Label timeValue = new Label("0 s");
    private File chosenFile;
    private CsvImportTask currentTask;
    private long startNanos;

    public ImportScreen(PlanStore store) {
        this.store = store;
        chooseButton.setId("chooseButton");
        chooseButton.getStyleClass().add("pnp-button");
        fileLabel.setId("fileLabel");
        fileLabel.getStyleClass().add("pnp-muted");
        Label dropText = new Label("Drop a .csv file here");
        dropText.getStyleClass().add("pnp-drop-title");
        uploadIndicator.setId("uploadIndicator");
        uploadIndicator.getStyleClass().add("pnp-chip");
        indicate("No CSV uploaded yet", "chip-neutral", false);
        dropZone.getChildren().addAll(Theme.icon("dishes", 64), uploadIndicator, dropText, Theme.text("or", "pnp-muted"), chooseButton, fileLabel);
        dropZone.setAlignment(Pos.CENTER);
        dropZone.getStyleClass().add("pnp-drop");

        importButton.setId("importButton");
        importButton.getStyleClass().add("pnp-button");
        importButton.setDisable(true);
        cancelButton.setId("cancelButton");
        cancelButton.getStyleClass().add("pnp-button-secondary");
        cancelButton.setDisable(true);
        bar.setId("importBar");
        bar.getStyleClass().add("pnp-progress");
        bar.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(bar, Priority.ALWAYS);
        Label percent = new Label();
        percent.setMinWidth(44);
        percent.setStyle("-fx-font-weight: bold;");
        percent.textProperty().bind(Bindings.createStringBinding(
                () -> bar.getProgress() < 0 ? "" : Math.round(bar.getProgress() * 100) + "%", bar.progressProperty()));
        HBox progressRow = new HBox(10, bar, percent);
        progressRow.setAlignment(Pos.CENTER_LEFT);
        statusLabel.setId("importStatus");
        statusLabel.getStyleClass().add("pnp-status-banner");   // green when the import worked, red when it failed
        statusLabel.setMaxWidth(Double.MAX_VALUE);
        resultsRow.getChildren().addAll(Theme.statCard("ROWS IMPORTED", importedValue, new Label("meals added to your plan"), "dishes"),
                Theme.statCard("BAD ROWS SKIPPED", skippedValue, new Label("never stop the import"), "food"),
                Theme.statCard("TIME TAKEN", timeValue, new Label("on a background thread"), "streak"));
        Theme.show(resultsRow, false);

        Label example = Theme.text("date,recipe_name,category,prep_minutes,meal_slot,calories,healthy_level\n"
                + "2026-11-03,Chicken Adobo,MainCourse,45,Dinner,620,Healthy\n"
                + "2026-11-04,Leche Flan,Dessert,50,Snack,350,Indulgent\n"
                + "2026-11-05,Tapsilog,Breakfast,20", "pnp-mono");
        Label note = Theme.text("The last three columns are optional, so old 4-column files still work. Without them a meal "
                + "gets the slot of its type and its healthy level is worked out from the calories. Rows with a wrong date, "
                + "a missing name, a bad number or an unknown slot or level are skipped and counted.", "pnp-muted");
        note.setWrapText(true);

        root.setPadding(new Insets(22, 26, 26, 26));
        root.getChildren().addAll(
                card(stepTitle(1, "Pick your file"), dropZone),
                card(stepTitle(2, "Import it"), new HBox(10, importButton, cancelButton), progressRow,
                        statusLabel),
                resultsRow,
                card(Theme.text("What the CSV must look like", "pnp-section"), example, note));
        scroller.setFitToWidth(true);
        scroller.getStyleClass().add("pnp-scroll");
        Theme.apply(scroller);
        Theme.status(statusLabel, "Choose a CSV file to begin.", Theme.Kind.INFO);
        wireEvents();
    }

    public Parent getView() {
        return scroller;
    }

    public void setOnImportFinished(Runnable onImportFinished) {
        this.onImportFinished = onImportFinished;
    }

    /** Stops a running import (when the user logs out or closes the window). */
    public void cancelImport() {
        if (currentTask != null) {
            currentTask.cancel();
        }
    }

    /** "1  Pick your file": the step number in a burgundy circle, then the step's name. */
    private static HBox stepTitle(int number, String text) {
        HBox title = new HBox(10, Theme.text(String.valueOf(number), "pnp-step-number"), Theme.text(text, "pnp-section"));
        title.setAlignment(Pos.CENTER_LEFT);
        return title;
    }

    private static VBox card(Node... children) {
        VBox card = new VBox(10, children);
        card.getStyleClass().add("pnp-card");
        return card;
    }

    private void wireEvents() {
        chooseButton.setOnAction(e -> chooseFile());
        importButton.setOnAction(e -> startImport());
        cancelButton.setOnAction(e -> cancelImport());
        dropZone.setOnDragOver(e -> {
            if (currentTask == null && e.getDragboard().hasFiles()) {
                e.acceptTransferModes(TransferMode.COPY);
                Theme.addClass(dropZone, "drag-over");
            }
            e.consume();
        });
        dropZone.setOnDragExited(e -> dropZone.getStyleClass().remove("drag-over"));
        dropZone.setOnDragDropped(e -> {
            List<File> csv = e.getDragboard().getFiles().stream()
                    .filter(f -> f.getName().toLowerCase(Locale.ROOT).endsWith(".csv")).toList();
            if (currentTask == null && !csv.isEmpty()) {
                pick(csv.get(0));
            } else {
                Theme.status(statusLabel, "Please drop a .csv file.", Theme.Kind.ERROR);
            }
            e.setDropCompleted(!csv.isEmpty());
            e.consume();
        });
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose meal-history CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File samples = new File("sample_data");
        if (samples.isDirectory()) {
            chooser.setInitialDirectory(samples);
        }
        File file = chooser.showOpenDialog(chooseButton.getScene().getWindow());
        if (file != null) {
            pick(file);
        }
    }

    /** Sets the indicator's text and color, and gives the drop zone a green edge while a file is loaded. */
    private void indicate(String text, String chipClass, boolean hasFile) {
        uploadIndicator.setText(text);
        uploadIndicator.getStyleClass().removeAll("chip-neutral", "chip-healthy", "chip-moderate", "chip-indulgent");
        uploadIndicator.getStyleClass().add(chipClass);
        dropZone.getStyleClass().remove("has-file");
        if (hasFile) {
            dropZone.getStyleClass().add("has-file");
        }
    }

    private void pick(File file) {
        chosenFile = file;
        indicate("\u2713 CSV uploaded: " + file.getName() + " is ready to import", "chip-healthy", true);
        fileLabel.setText(file.getName() + "   (" + String.format(Locale.ENGLISH, "%.1f KB", file.length() / 1024.0) + ")");
        importButton.setDisable(false);
        Theme.show(resultsRow, false);
        bar.setProgress(0);
        Theme.status(statusLabel, "Ready. Press Import.", Theme.Kind.INFO);
    }

    private void startImport() {
        CsvImportTask task = new CsvImportTask(chosenFile, store);
        currentTask = task;
        startNanos = System.nanoTime();
        chooseButton.setDisable(true);
        importButton.setDisable(true);
        cancelButton.setDisable(false);
        Theme.show(resultsRow, false);
        indicate("Importing " + chosenFile.getName() + "...", "chip-moderate", true);
        bar.progressProperty().bind(task.progressProperty());     // the bar follows the Task
        statusLabel.textProperty().bind(task.messageProperty());
        String name = chosenFile.getName();
        task.setOnSucceeded(e -> {
            finish(task, "Done! The import finished.", 1.0, Theme.Kind.OK, true);
            indicate("\u2713 Imported " + String.format(Locale.ENGLISH, "%,d", task.getImported()) + " rows from " + name,
                    "chip-healthy", true);
        });
        task.setOnFailed(e -> {
            finish(task, "Import failed: " + task.getException().getMessage(), 0, Theme.Kind.ERROR, false);
            indicate("Import failed for " + name, "chip-indulgent", false);
        });
        task.setOnCancelled(e -> {
            finish(task, "Import cancelled.", 0, Theme.Kind.INFO, true);
            indicate("Import cancelled: " + name + " is still loaded", "chip-neutral", true);
        });
        AppShell.runInBackground(task);                             // BACKGROUND THREAD: the UI stays responsive
    }

    private void finish(CsvImportTask task, String message, double progress, Theme.Kind kind, boolean showNumbers) {
        statusLabel.textProperty().unbind();
        bar.progressProperty().unbind();
        Theme.status(statusLabel, message, kind);
        bar.setProgress(progress);
        if (showNumbers) {
            importedValue.setText(String.format(Locale.ENGLISH, "%,d", task.getImported()));
            skippedValue.setText(String.format(Locale.ENGLISH, "%,d", task.getSkipped()));
            timeValue.setText(String.format(Locale.ENGLISH, "%.1f s", (System.nanoTime() - startNanos) / 1_000_000_000.0));
            Theme.show(resultsRow, true);
        }
        chooseButton.setDisable(false);
        importButton.setDisable(chosenFile == null);
        cancelButton.setDisable(true);
        currentTask = null;
        onImportFinished.run();
    }
}
