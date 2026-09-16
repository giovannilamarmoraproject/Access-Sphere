package io.github.giovannilamarmora.accesssphere.data.user.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangePassword {
  private String templateId;
  private String email;
  private String password;
  private String token;
  private String clientId;
  private Map<String, String> params;

  public ChangePassword(
      String templateId,
      String email,
      String password,
      String token,
      Map<String, String> params) {
    this.templateId = templateId;
    this.email = email;
    this.password = password;
    this.token = token;
    this.params = params;
  }
}
