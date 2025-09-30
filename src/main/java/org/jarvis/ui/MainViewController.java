package org.jarvis.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;

public class MainViewController {

    @FXML private TableView<Rule> rulesTable;
    @FXML private TableColumn<Rule, Integer> colRuleId;
    @FXML private TableColumn<Rule, String> colRuleType;
    @FXML private TableColumn<Rule, String> colRuleValue;
    @FXML private TableColumn<Rule, Boolean> colRuleEnabled;
    @FXML private ListView<String> logListView;

    private DatabaseManager databaseManager;
    private ObservableList<Rule> ruleList;

    // This method is automatically called after the FXML file has been loaded
    public void initialize() {
        this.databaseManager = new DatabaseManager();

        // 1. Set up the columns in the table
        colRuleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRuleType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRuleValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colRuleEnabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));

        // 2. Load data from the database
        loadRules();
    }

    private void loadRules() {
        // Use FXCollections.observableArrayList to create a list that the TableView can observe for changes.
        ruleList = FXCollections.observableArrayList(databaseManager.getAllActiveRules());
        rulesTable.setItems(ruleList);
    }

    @FXML
    private void handleAddRule() {
        // We will implement the pop-up dialog for this in the next step.
        System.out.println("Add Rule button clicked.");
    }

    @FXML
    private void handleDeleteRule() {
        // We will implement this later.
        System.out.println("Delete Rule button clicked.");
    }

    // This method can be called from the PacketAnalyzer to update the UI with new logs.
    public void addLogMessage(String message) {
        // To prevent blocking the UI, updates must happen on the JavaFX Application Thread.
        javafx.application.Platform.runLater(() -> {
            logListView.getItems().add(0, message); // Add to the top of the list
        });
    }
}