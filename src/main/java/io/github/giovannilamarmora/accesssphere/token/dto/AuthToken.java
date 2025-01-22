package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuthToken {

  private String id_token;
  private String access_token;
  private String refresh_token;
  private Long expires_at;
  private Long expires;
  private String token_type;
}
