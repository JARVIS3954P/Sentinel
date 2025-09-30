package org.jarvis.ui;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.jarvis.model.Rule;

public class AddRuleController {

    @FXML private TextField ipAddressField;

    private Stage dialogStage;
    private Rule newRule = null;
    private boolean saved = false;

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
        String ip = ipAddressField.getText();
        if (ip != null && !ip.trim().isEmpty()) {
            // We create a temporary rule object to pass back.
            // The ID will be assigned by the database.
            newRule = new Rule(0, "IP_BLOCK", ip, true);
            saved = true;
            dialogStage.close();
        } else {
            // You can add an alert here to show an error
            System.err.println("IP address cannot be empty.");
        }
    }

    @FXML
    private void handleCancel() {
        dialogStage.close();
    }
}