package io.github.giovannilamarmora.accesssphere.token.dto;

public enum TokenClaims {
    ID("id"),
    EMAIL("email"),
    ROLE("role"),
    AUTH_TYPE("auth_type"),
    GOOGLE_TOKEN("google_token");

    private final String claim;

    TokenClaims(String claim) {
        this.claim = claim;
    }

    public String claim() {
        return claim;
    }
}
