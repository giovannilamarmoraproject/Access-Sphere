package io.github.giovannilamarmora.accesssphere.client.model;

public enum AccessType {
    ONLINE("online"),
    OFFLINE("offline");

    private final String value;

    AccessType(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
