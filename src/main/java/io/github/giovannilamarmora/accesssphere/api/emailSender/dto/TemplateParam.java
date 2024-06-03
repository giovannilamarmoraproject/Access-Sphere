package io.github.giovannilamarmora.accesssphere.api.emailSender.dto;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import java.util.HashMap;
import java.util.Map;
import lombok.Getter;

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
    return param;
  }
}
