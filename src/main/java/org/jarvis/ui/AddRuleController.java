package org.jarvis.ui;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jarvis.model.Rule;

public class AddRuleController {

    @FXML private TextField valueField;
    @FXML private ComboBox<String> directionComboBox;

    private Stage dialogStage;
    private Rule newRule = null;
    private boolean saved = false;

    @FXML
    public void initialize() {
        directionComboBox.setItems(FXCollections.observableArrayList("Incoming", "Outgoing", "Both"));
        directionComboBox.getSelectionModel().select("Outgoing");
    }

    public void setDialogStage(Stage dialogStage) {
        this.dialogStage = dialogStage;
    }

    public boolean isSaved() {
        return saved;
    }

    public Rule getNewRule() {
        return newRule;
    }

    @FXML
    private void handleSave() {
        String value = valueField.getText();
        String direction = directionComboBox.getSelectionModel().getSelectedItem();

        if (value != null && !value.trim().isEmpty()) {
            newRule = new Rule(0, "IP_BLOCK", value, direction, true);
            saved = true;
            dialogStage.close();
        } else {
            System.err.println("IP or Hostname cannot be empty.");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}