package org.jarvis.ui;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jarvis.core.DomainScraper;
import org.jarvis.core.PacketListenerService;
import org.jarvis.enforcer.HostsFileManager;
import org.jarvis.model.Rule;
import org.jarvis.persistence.DatabaseManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class MainViewController {

    // --- UI Fields ---
    @FXML private TableView<Rule> rulesTable;
    @FXML private TableColumn<Rule, Integer> colRuleId;
    @FXML private TableColumn<Rule, String> colRuleType;
    @FXML private TableColumn<Rule, String> colRuleValue;
    @FXML private TableColumn<Rule, String> colRuleDirection;
    @FXML private TableColumn<Rule, Boolean> colRuleEnabled;
    @FXML private ListView<String> logListView;
    @FXML private Label totalBlockedLabel;
    @FXML private Label activeRulesLabel;
    @FXML private PieChart directionPieChart;

    // --- Backend Services ---
    private PacketListenerService packetListenerService;
    private DatabaseManager databaseManager;
    private HostsFileManager hostsFileManager;

    private ObservableList<Rule> ruleList;

    public void initialize() {
        this.databaseManager = new DatabaseManager();
        initializeRulesManagementTab();
        initializeDashboard();
    }

    private void initializeRulesManagementTab() {
        colRuleId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colRuleType.setCellValueFactory(new PropertyValueFactory<>("type"));
        colRuleValue.setCellValueFactory(new PropertyValueFactory<>("value"));
        colRuleDirection.setCellValueFactory(new PropertyValueFactory<>("direction"));
        colRuleEnabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        loadRules();
    }

    private void initializeDashboard() {
        updateDashboard();
    }

    public void setPacketListenerService(PacketListenerService service) {
        this.packetListenerService = service;
    }

    public void setHostsFileManager(HostsFileManager hostsFileManager) {
        this.hostsFileManager = hostsFileManager;
    }

    private void loadRules() {
        ruleList = FXCollections.observableArrayList(databaseManager.getAllActiveRules());
        rulesTable.setItems(ruleList);
        if (activeRulesLabel != null) {
            Platform.runLater(() -> activeRulesLabel.setText(String.valueOf(ruleList.size())));
        }
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
                Rule tempRule = controller.getNewRule();
                String input = tempRule.getValue().trim();
                String direction = tempRule.getDirection();
                new Thread(() -> processAddRuleRequest(input, direction)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void processAddRuleRequest(String input, String direction) {
        String ipPattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$";

        if (input.matches(ipPattern) || input.contains(":")) { // Simple check for IPv4 or IPv6
            addSingleIpRule(input, direction);
            Platform.runLater(this::loadRules);
        } else {
            // --- HYBRID BLOCKING FOR KEYWORDS ---
            addLogMessage("Starting dynamic block for keyword: '" + input + "'...");
            DomainScraper scraper = new DomainScraper();
            Set<String> domainsToBlock = scraper.findRelatedDomains(input);

            if (domainsToBlock.isEmpty()) {
                addLogMessage("No related domains found. Aborting.");
                return;
            }
            addLogMessage("Found " + domainsToBlock.size() + " related domains. Blocking domains and resolving all IPs...");

            Set<String> resolvedIps = new HashSet<>();
            for (String domain : domainsToBlock) {
                // Step 1: Add domain to hosts file for graceful blocking page
                hostsFileManager.addDomainBlock(domain);
                databaseManager.addRule("DOMAIN_BLOCK", domain, "N/A", true);

                // Step 2: Resolve all IPs for this domain for rigorous iptables blocking
                try {
                    InetAddress[] addresses = InetAddress.getAllByName(domain);
                    for (InetAddress addr : addresses) {
                        resolvedIps.add(addr.getHostAddress());
                    }
                } catch (UnknownHostException e) {
                    System.err.println("Could not resolve domain: " + domain);
                }
            }

            addLogMessage("Resolved " + resolvedIps.size() + " unique IP addresses. Adding firewall rules...");
            for (String ip : resolvedIps) {
                addSingleIpRule(ip, direction);
            }

            addLogMessage("Finished rigorous block for keyword: " + input);
            Platform.runLater(this::loadRules);
        }
    }

    private void addSingleIpRule(String ip, String direction) {
        databaseManager.addRule("IP_BLOCK", ip, direction, true);
        Rule newRuleForAnalyzer = new Rule(0, "IP_BLOCK", ip, direction, true);
        packetListenerService.getAnalyzer().addRule(newRuleForAnalyzer);
    }

    @FXML
    private void handleDeleteRule() {
        Rule selectedRule = rulesTable.getSelectionModel().getSelectedItem();
        if (selectedRule != null) {
            if ("IP_BLOCK".equals(selectedRule.getType())) {
                packetListenerService.getFirewallManager().unblockIp(selectedRule.getValue(), selectedRule.getDirection());
                packetListenerService.getAnalyzer().removeRule(selectedRule.getValue());
            } else if ("DOMAIN_BLOCK".equals(selectedRule.getType())) {
                hostsFileManager.removeDomainBlock(selectedRule.getValue());
            }

            databaseManager.deleteRule(selectedRule.getId());
            ruleList.remove(selectedRule);
            addLogMessage("Deleted rule for: " + selectedRule.getValue());
            updateDashboard();
        } else {
            showAlert(Alert.AlertType.WARNING, "No Selection", "No Rule Selected", "Please select a rule in the table to delete.");
        }
    }

    @FXML
    private void handleDeleteAllRules() {
        if (ruleList.isEmpty()) {
            showAlert(Alert.AlertType.INFORMATION, "No Rules", "There are no rules to delete.", "");
            return;
        }

        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle("Confirm Deletion");
        confirmationAlert.setHeaderText("Delete All Firewall Rules");
        confirmationAlert.setContentText("Are you sure you want to permanently delete all " + ruleList.size() + " rules? This action cannot be undone.");

        Optional<ButtonType> result = confirmationAlert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            new Thread(this::performDeleteAllRules).start();
        }
    }

    private void performDeleteAllRules() {
        addLogMessage("Starting deletion of all rules...");
        ArrayList<Rule> rulesToDelete = new ArrayList<>(ruleList);
        for (Rule rule : rulesToDelete) {
            if ("IP_BLOCK".equals(rule.getType())) {
                packetListenerService.getFirewallManager().unblockIp(rule.getValue(), rule.getDirection());
                packetListenerService.getAnalyzer().removeRule(rule.getValue());
            } else if ("DOMAIN_BLOCK".equals(rule.getType())) {
                hostsFileManager.removeDomainBlock(rule.getValue());
            }
        }
        databaseManager.deleteAllRules();
        Platform.runLater(() -> {
            addLogMessage("All rules have been successfully deleted.");
            loadRules();
        });
    }

    public void addLogMessage(String message) {
        Platform.runLater(() -> {
            logListView.getItems().add(0, message);
            updateDashboard();
        });
    }

    private void updateDashboard() {
        new Thread(() -> {
            int totalBlocked = databaseManager.getTotalBlockedCount();
            Map<String, Integer> topIps = databaseManager.getTopBlockedIPs(5);
            Map<String, Integer> directionData = databaseManager.getBlockedTrafficByDirection();

            Platform.runLater(() -> {
                totalBlockedLabel.setText(String.valueOf(totalBlocked));

                XYChart.Series<String, Number> ipSeries = new XYChart.Series<>();
                ipSeries.setName("Block Count");
                for (Map.Entry<String, Integer> entry : topIps.entrySet()) {
                    ipSeries.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
                }

                ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
                for (Map.Entry<String, Integer> entry : directionData.entrySet()) {
                    pieChartData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
                }
                directionPieChart.setData(pieChartData);
            });
        }).start();
    }

    private void showAlert(Alert.AlertType alertType, String title, String header, String content) {
        Alert alert = new Alert(alertType);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}