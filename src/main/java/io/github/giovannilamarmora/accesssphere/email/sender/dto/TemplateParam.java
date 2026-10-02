package io.github.giovannilamarmora.accesssphere.email.sender.dto;

import io.github.giovannilamarmora.accesssphere.user.model.User;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

/**
 * Utility per l'estrazione e associazione standard dei parametri di contesto utente
 * ai segnaposto {{KEY}} dei template email.
 */
@Getter
public enum TemplateParam {
  USER_RESET_TOKEN("USER.RESET_TOKEN");

  private final String value;

  TemplateParam(String value) {
    this.value = value;
  }

  public static Map<String, String> getTemplateParam(User user) {
    Map<String, String> param = new HashMap<>();
    param.put("USER.NAME", user.getName());
    param.put("USER.SURNAME", user.getSurname());
    param.put("USER.EMAIL", user.getEmail());
    param.put("USER.RESET_TOKEN", user.getTokenReset());
    param.put("NAME", user.getName());
    param.put("RESET_CODE", user.getTokenReset());
    return param;
  }
}
