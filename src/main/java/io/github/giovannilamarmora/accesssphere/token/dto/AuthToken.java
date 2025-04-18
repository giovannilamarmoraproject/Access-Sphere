package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthToken {

  private String id_token;
  private String identifier;
  private String subject;
  private TempToken temp_token;
  private String access_token;
  private String refresh_token;
  private Long expires_at;
  private Long expires;
  private String token_type;
  private List<String> mfa_methods;

  public AuthToken(
      String id_token,
      String access_token,
      String refresh_token,
      Long expires_at,
      Long expires,
      String token_type) {
    this.id_token = id_token;
    this.access_token = access_token;
    this.refresh_token = refresh_token;
    this.expires_at = expires_at;
    this.expires = expires;
    this.token_type = token_type;
  }

  public AuthToken(
      String identifier, String subject, TempToken temp_token, List<String> mfa_methods) {
    this.identifier = identifier;
    this.subject = subject;
    this.temp_token = temp_token;
    this.mfa_methods = mfa_methods;
  }
}
