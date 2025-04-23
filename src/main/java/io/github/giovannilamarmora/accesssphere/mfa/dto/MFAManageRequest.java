package io.github.giovannilamarmora.accesssphere.mfa.dto;

public record MFAManageRequest(String identifier, MFALabel label, Action action) {
  public enum Action {
    ENABLE,
    DISABLE,
    DELETE
  }
}
