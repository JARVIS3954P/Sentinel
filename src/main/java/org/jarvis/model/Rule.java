package org.jarvis.model;

public class Rule {
    private final int id;
    private final String type;
    private final String value;
    private final boolean enabled;

    public Rule(int id, String type, String value, boolean enabled) {
        this.id = id;
        this.type = type;
        this.value = value;
        this.enabled = enabled;
    }

    public String getValue() {
        return value;
    }
    // You can add more getters if needed
}