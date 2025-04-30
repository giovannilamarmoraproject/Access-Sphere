package io.github.giovannilamarmora.accesssphere.mfa.dto;

public record MFAManageRequest(String identifier, TOTPLabel label, Action action) {
  public enum Action {
    ENABLE,
    DISABLE,
    DELETE
  }
}
