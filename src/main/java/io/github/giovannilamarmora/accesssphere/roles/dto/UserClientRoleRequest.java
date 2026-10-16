package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleHolder;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Modello DTO di richiesta per l'assegnazione di un ruolo applicativo contestualizzato a uno
 * specifico Client OAuth2.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Assignment request of an application role for an OAuth2 Client")
public class UserClientRoleRequest implements RoleHolder {

  @NotBlank(message = "The field 'clientId' is required and it cannot be null or blank")
  @UpperCase
  @Schema(
      description = "Identifier of the OAuth2 Client associated with the role",
      example = "LINKATUTTO-ADMIN")
  private String clientId;

  @NotBlank(message = "The field 'role' is required and it cannot be null or blank")
  @UpperCase
  @Schema(description = "Identifier name of the role to assign to the user", example = "ROLE_USER")
  private String role;
}
