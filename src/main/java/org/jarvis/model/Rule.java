package org.jarvis.model;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Rule {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty type;
    private final SimpleStringProperty value;
    private final SimpleBooleanProperty enabled;

    public Rule(int id, String type, String value, boolean enabled) {
        this.id = new SimpleIntegerProperty(id);
        this.type = new SimpleStringProperty(type);
        this.value = new SimpleStringProperty(value);
        this.enabled = new SimpleBooleanProperty(enabled);
    }

    // --- Getters for JavaFX Properties ---
    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }
    public String getType() { return type.get(); }
    public SimpleStringProperty typeProperty() { return type; }
    public String getValue() { return value.get(); }
    public SimpleStringProperty valueProperty() { return value; }
    public boolean isEnabled() { return enabled.get(); }
    public SimpleBooleanProperty enabledProperty() { return enabled; }
}