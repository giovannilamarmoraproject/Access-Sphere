package io.github.giovannilamarmora.accesssphere.token.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TokenExchange {

  @NotBlank(message = "You must use a valid grant_type")
  private String grant_type;

  @NotBlank(message = "Subject token cannot be blank")
  private String subject_token;

  @NotBlank(message = "Requested token type cannot be blank")
  private String requested_token_type;

  @NotBlank(message = "Client ID must be provided")
  private String client_id;

  @NotBlank(message = "Scope must be provided")
  private String scope;
}
