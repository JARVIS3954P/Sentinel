package org.jarvis.ui;


import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.layout.GridPane;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;
import org.jarvis.core.PacketListenerService;

public class MainViewController {

    @FXML private TableView<Rule> rulesTable;
    @FXML private TableColumn<Rule, Integer> colRuleId;
    @FXML private TableColumn<Rule, String> colRuleType;
    @FXML private TableColumn<Rule, String> colRuleValue;
    @FXML private TableColumn<Rule, Boolean> colRuleEnabled;
    @FXML private ListView<String> logListView;
    private PacketListenerService packetListenerService;

    private DatabaseManager databaseManager;
    private ObservableList<Rule> ruleList;

    //link the service
    public void setPacketListenerService(PacketListenerService service) {
        this.packetListenerService = service;
    }

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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/org/jarvis/ui/add-rule-view.fxml"));
            GridPane page = loader.load();

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Add New Rule");
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.initOwner(rulesTable.getScene().getWindow());
            Scene scene = new Scene(page);
            dialogStage.setScene(scene);

            AddRuleController controller = loader.getController();
            controller.setDialogStage(dialogStage);

            dialogStage.showAndWait();

            if (controller.isSaved()) {
                Rule newRule = controller.getNewRule();
                // 1. Save to database
                databaseManager.addRule(newRule.getType(), newRule.getValue(), newRule.isEnabled());
                // 2. Inform the live analyzer
                packetListenerService.getAnalyzer().addRule(newRule.getValue());
                // 3. Refresh the UI table
                loadRules();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDeleteRule() {
        Rule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
        if (selectedRule != null) {
            // Here you would add a confirmation dialog in a real app

            // 1. Delete from database
            databaseManager.deleteRule(selectedRule.getId()); // You need to add this method!

            // 2. Unblock from the firewall
            packetListenerService.getFirewallManager().unblockIp(selectedRule.getValue()); // You need to add this getter!

            // 3. Inform the live analyzer
            packetListenerService.getAnalyzer().removeRule(selectedRule.getValue());

            // 4. Refresh the UI table
            ruleList.remove(selectedRule);
        } else {
            // Show an error alert
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("No Selection");
            alert.setHeaderText("No Rule Selected");
            alert.setContentText("Please select a rule in the table to delete.");
            alert.showAndWait();
        }
    }

    // This method can be called from the PacketAnalyzer to update the UI with new logs.
    public void addLogMessage(String message) {
        // To prevent blocking the UI, updates must happen on the JavaFX Application Thread.
        javafx.application.Platform.runLater(() -> {
            logListView.getItems().add(0, message); // Add to the top of the list
        });
    }
}