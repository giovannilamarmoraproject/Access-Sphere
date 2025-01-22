package io.github.giovannilamarmora.accesssphere.client.model;

public enum TokenType {
    BEARER_JWT("BEARER_JWT"),
    BEARER_JWE("BEARER_JWE");

    private final String type;

    TokenType(String type) {
        this.type = type;
    }

    public String type() {
        return type;
    }
}
