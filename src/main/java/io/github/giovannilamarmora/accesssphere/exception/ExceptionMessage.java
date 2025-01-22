package io.github.giovannilamarmora.accesssphere.exception;

import lombok.Getter;

@Getter
public enum ExceptionMessage {
  AUTHORIZATION_HEADER("Missing Authorization Header"),
  SESSION_SHOULD_BE("Invalid session_id, should be {} instead to {}"),
  NO_SESSION_ID("No Session ID Provided!"),
  INVALID_SESSION_ID("Invalid Session ID Provided!");

  private final String message;

  ExceptionMessage(String message) {
    this.message = message;
  }
}
