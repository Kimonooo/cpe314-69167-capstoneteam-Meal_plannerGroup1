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
 */
public class ImportScreen {
    private final PlanStore store;
    private Runnable onImportFinished = () -> { };
    private final VBox root = new VBox(14);
    private final ScrollPane scroller = new ScrollPane(root);
    private final VBox dropZone = new VBox(10);
    private final Button chooseButton = new Button("Choose CSV...");
    private final Label fileLabel = new Label("No file chosen yet");
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
        chooseButton.getStyleClass().add("pnp-button-secondary");
        fileLabel.setId("fileLabel");
        fileLabel.getStyleClass().add("pnp-muted");
        Label dropText = new Label("Drop a .csv file here");
        dropText.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        dropZone.getChildren().addAll(dropText, Theme.text("or", "pnp-muted"), chooseButton, fileLabel);
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
        resultsRow.getChildren().addAll(Theme.statCard("ROWS IMPORTED", importedValue, new Label("meals added to your plan")),
                Theme.statCard("BAD ROWS SKIPPED", skippedValue, new Label("never stop the import")),
                Theme.statCard("TIME TAKEN", timeValue, new Label("on a background thread")));
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
                Theme.pageHeader("Import Meal History",
                        "Load thousands of past meals from a CSV file. It runs in the background, so you can keep using the app."),
                card(Theme.text("Step 1: Pick your file", "pnp-section"), dropZone),
                card(Theme.text("Step 2: Import it", "pnp-section"), new HBox(10, importButton, cancelButton), progressRow,
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

    private void pick(File file) {
        chosenFile = file;
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
        bar.progressProperty().bind(task.progressProperty());     // the bar follows the Task
        statusLabel.textProperty().bind(task.messageProperty());
        task.setOnSucceeded(e -> finish(task, "Done! The import finished.", 1.0, Theme.Kind.OK, true));
        task.setOnFailed(e -> finish(task, "Import failed: " + task.getException().getMessage(), 0, Theme.Kind.ERROR, false));
        task.setOnCancelled(e -> finish(task, "Import cancelled.", 0, Theme.Kind.INFO, true));
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
