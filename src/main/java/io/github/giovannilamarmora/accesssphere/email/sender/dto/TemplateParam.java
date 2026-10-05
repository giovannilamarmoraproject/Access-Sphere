package io.github.giovannilamarmora.accesssphere.email.sender.dto;

import io.github.giovannilamarmora.accesssphere.email.sender.EmailParameterResolver;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import java.util.Map;
import lombok.Getter;

/**
 * Utility per l'estrazione e associazione standard dei parametri di contesto utente
 * ai segnaposto {{KEY}} dei template email.
 * <p>
 * Interconnesso con {@link EmailParameterResolver} per l'espansione unificata dei namespace.
 */
@Getter
public enum TemplateParam {
  USER_RESET_TOKEN("USER.RESET_TOKEN");

  private final String value;

  TemplateParam(String value) {
    this.value = value;
  }

  public static Map<String, String> getTemplateParam(User user) {
    return EmailParameterResolver.resolve(user, null);
  }
}
