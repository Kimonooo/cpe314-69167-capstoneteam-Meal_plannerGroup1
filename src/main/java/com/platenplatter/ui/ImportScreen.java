package com.platenplatter.ui;

import com.platenplatter.util.RowSink;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;

/**
 * "Import" tab. Responsibility: pick a CSV and run CsvImportTask on a BACKGROUND THREAD.
 * The ProgressBar and status label are bound to the task, so the UI never freezes and
 * the user can click other tabs while the import runs.
 * Controls (all wired): Choose CSV, Import, Cancel.
 */
public class ImportScreen {
    private final RowSink sink;

    private final VBox root = new VBox(12);
    private final Button chooseButton = new Button("Choose CSV...");
    private final Button importButton = new Button("Import");
    private final Button cancelButton = new Button("Cancel");
    private final Label fileLabel = new Label("No file chosen");
    private final ProgressBar bar = new ProgressBar(0);
    private final Label status = new Label("Choose a meal-history CSV (date,recipe_name,category,prep_minutes).");

    private File chosenFile;
    private CsvImportTask currentTask;

    public ImportScreen(RowSink sink) {
        this.sink = sink;

        Label title = new Label("Import Meal History");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #5C0008;");

        styleButton(chooseButton);
        styleButton(importButton);
        styleButton(cancelButton);
        importButton.setDisable(true);
        cancelButton.setDisable(true);
        bar.setPrefWidth(420);

        HBox buttons = new HBox(10, chooseButton, importButton, cancelButton);
        root.getChildren().addAll(title, buttons, fileLabel, bar, status);
        root.setPadding(new Insets(20));
        root.setStyle("-fx-background-color: #FFFBD0;");

        chooseButton.setOnAction(e -> chooseFile());
        importButton.setOnAction(e -> startImport());
        cancelButton.setOnAction(e -> {
            if (currentTask != null) {
                currentTask.cancel();
            }
        });
    }

    public Parent getView() {
        return root;
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choose meal-history CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV files", "*.csv"));
        File file = chooser.showOpenDialog(chooseButton.getScene().getWindow());
        if (file != null) {
            chosenFile = file;
            fileLabel.setText("Selected: " + file.getName());
            importButton.setDisable(false);
        }
    }

    private void startImport() {
        if (chosenFile == null) {
            status.setText("Choose a CSV file first.");
            return;
        }
        CsvImportTask task = new CsvImportTask(chosenFile, sink);
        currentTask = task;

        chooseButton.setDisable(true);
        importButton.setDisable(true);
        cancelButton.setDisable(false);
        bar.progressProperty().bind(task.progressProperty());
        status.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> finish("Done! Imported " + task.getValue()
                + " rows, skipped " + task.getSkipped() + " bad rows.", 1.0));
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            finish("Import failed: " + (ex == null ? "unknown error" : ex.getMessage()), 0);
        });
        task.setOnCancelled(e -> finish("Import cancelled.", 0));

        Thread thread = new Thread(task); // BACKGROUND THREAD - UI stays responsive
        thread.setDaemon(true);
        thread.start();
    }

    private void finish(String message, double progress) {
        status.textProperty().unbind();
        bar.progressProperty().unbind();
        status.setText(message);
        bar.setProgress(progress);
        chooseButton.setDisable(false);
        importButton.setDisable(chosenFile == null);
        cancelButton.setDisable(true);
        currentTask = null;
    }

    private static void styleButton(Button b) {
        b.setStyle("-fx-background-color: #5C0008; -fx-text-fill: white;");
    }
}
