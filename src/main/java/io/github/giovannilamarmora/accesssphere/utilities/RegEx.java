package io.github.giovannilamarmora.accesssphere.utilities;

public enum RegEx {
  EMAIL("^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$"),
  PASSWORD_FULL("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&.,])[A-Za-z\\d@$!%*?&.,]{8,20}$");

  private String value;

  RegEx(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }
}
