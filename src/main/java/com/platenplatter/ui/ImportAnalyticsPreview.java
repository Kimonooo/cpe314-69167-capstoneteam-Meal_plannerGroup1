package com.platenplatter.ui;

import com.platenplatter.util.ImportRow;
import com.platenplatter.util.Loader;
import com.platenplatter.util.RowSink;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Lets Yvonne test Import + Analytics together WITHOUT the database:
 * imported rows go into an in-memory list. Run on the LAPTOP:
 *   mvn javafx:run -Dapp.mainClass=com.platenplatter.ui.ImportAnalyticsPreview
 * Try: import sample_data/meal_history_3000.csv, switch to Analytics, press Refresh.
 */
public class ImportAnalyticsPreview extends Application {
    @Override
    public void start(Stage stage) {
        List<ImportRow> store = Collections.synchronizedList(new ArrayList<>());
        RowSink sink = store::add;
        Loader<List<String>> categories = () -> {
            List<String> out = new ArrayList<>();
            synchronized (store) {
                for (ImportRow r : store) {
                    out.add(r.getCategory());
                }
            }
            return out;
        };
        Loader<List<Integer>> minutes = () -> {
            List<Integer> out = new ArrayList<>();
            synchronized (store) {
                for (ImportRow r : store) {
                    out.add(r.getPrepMinutes());
                }
            }
            return out;
        };

        TabPane tabs = new TabPane();
        tabs.getTabs().add(new Tab("Import", new ImportScreen(sink).getView()));
        tabs.getTabs().add(new Tab("Analytics", new AnalyticsScreen(categories, minutes).getView()));
        tabs.getTabs().forEach(t -> t.setClosable(false));
        stage.setTitle("Import + Analytics preview");
        stage.setScene(new Scene(tabs, 800, 600));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
