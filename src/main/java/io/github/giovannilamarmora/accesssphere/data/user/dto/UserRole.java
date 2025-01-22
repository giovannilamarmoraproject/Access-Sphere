package io.github.giovannilamarmora.accesssphere.data.user.dto;

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

  public String getDisplayName() {
    return displayName;
  }
}
