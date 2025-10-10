package org.jarvis.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jarvis.core.PacketListenerService;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

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

    /**
     * This method is automatically called by JavaFX after the FXML file has been loaded.
     * It's the perfect place for initialization.
     */
    public void initialize() {
        this.databaseManager = new DatabaseManager();

        // 1. Set up the table columns to bind to the Rule model's properties.
        colRuleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRuleType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRuleValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colRuleEnabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));

        // 2. Load the initial set of rules from the database.
        loadRules();
    }

    /**
     * Links this controller to the main backend service.
     * This is called from the App class during startup.
     * @param service The running instance of the PacketListenerService.
     */
    public void setPacketListenerService(PacketListenerService service) {
        this.packetListenerService = service;
    }

    /**
     * Fetches all rules from the database and populates the TableView.
     */
    private void loadRules() {
        // Use FXCollections.observableArrayList to create a list that the TableView
        // can observe for changes, allowing for automatic UI updates.
        ruleList = FXCollections.observableArrayList(databaseManager.getAllActiveRules());
        rulesTable.setItems(ruleList);
    }

    /**
     * Handles the "Add Rule..." button click. Opens a dialog to get user input,
     * then resolves it as either an IP or a hostname.
     */
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

            // Show the dialog and wait until the user closes it
            dialogStage.showAndWait();

            // Process the result only if the user clicked "Save"
            if (controller.isSaved()) {
                Rule tempRule = controller.getNewRule();
                String input = tempRule.getValue();

                //Check if input is an IP or a Hostname
                String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

                if (input.matches(ipPattern)) {
                    // The input is already an IP address.
                    databaseManager.addRule("IP_BLOCK", input, true);
                    packetListenerService.getAnalyzer().addRule(input);
                    addLogMessage("Successfully added IP rule: " + input);
                } else {
                    // The input is a hostname, so we perform DNS resolution.
                    try {
                        InetAddress[] addresses = InetAddress.getAllByName(input);
                        int count = 0;
                        for (InetAddress addr : addresses) {
                            String ip = addr.getHostAddress();
                            databaseManager.addRule("IP_BLOCK", ip, true);
                            packetListenerService.getAnalyzer().addRule(ip);
                            count++;
                        }
                        addLogMessage("Successfully added " + count + " IP rule(s) for host: " + input);
                    } catch (UnknownHostException e) {
                        addLogMessage("Error: Could not resolve host '" + input + "'");
                        showAlert(Alert.AlertType.ERROR, "DNS Error", "Unknown Host",
                                "Could not find any IP addresses for the hostname: " + input);
                    }
                }
                // Refresh the table to show the new rule(s)
                loadRules();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Handles the "Delete Selected Rule" button click. Removes the rule from the
     * database, the live analyzer, and the system firewall.
     */
    @FXML
    private void handleDeleteRule() {
        Rule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
        if (selectedRule != null) {
            // 1. Delete from the database
            databaseManager.deleteRule(selectedRule.getId());

            // 2. Unblock the IP from the system firewall
            packetListenerService.getFirewallManager().unblockIp(selectedRule.getValue());

            // 3. Remove the rule from the live analyzer's ruleset
            packetListenerService.getAnalyzer().removeRule(selectedRule.getValue());

            // 4. Remove the rule from the UI table (which is bound to ruleList)
            ruleList.remove(selectedRule);

            addLogMessage("Deleted rule for IP: " + selectedRule.getValue());
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "No Rule Selected",
                    "Please select a rule in the table to delete.");
        }
    }

    /**
     * A thread-safe method for adding messages to the log view from any thread.
     * @param message The log message to display.
     */
    public void addLogMessage(String message) {
        // All UI updates must happen on the JavaFX Application Thread.
        // Platform.runLater() ensures this.
        javafx.application.Platform.runLater(() -> {
            logListView.getItems().add(0, message); // Add to the top for a live-log feel
        });
    }

    /**
     * Helper method to display a standardized alert dialog.
     */
    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}