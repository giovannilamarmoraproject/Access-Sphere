package io.github.giovannilamarmora.accesssphere.roles.dto;

import lombok.Getter;

@Getter
public enum UserRole {
  SUPER_ADMIN("Super Admin"),
  ADMIN("Administrator"),
  USER("User"),
  MANAGER("Manager"),
  MODERATOR("Moderator"),
  GUEST("Guest");

  private final String displayName;

  UserRole(String displayName) {
    this.displayName = displayName;
  }
}
