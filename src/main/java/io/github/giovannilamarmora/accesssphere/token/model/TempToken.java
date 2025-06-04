package io.github.giovannilamarmora.accesssphere.token.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TempToken {
  private String access_token;
  private Long expires_at;
  private String token_type;
}
