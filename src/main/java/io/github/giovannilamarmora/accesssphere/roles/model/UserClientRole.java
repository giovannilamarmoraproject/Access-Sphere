package io.github.giovannilamarmora.accesssphere.roles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.github.giovannilamarmora.utils.jsonSerialize.UpperCase;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio primario che rappresenta un ruolo contestualizzato assegnato a un utente per
 * uno specifico Client OAuth2, arricchito con la tipologia applicativa e la lista dei permessi
 * granulari aggregati. Mappa direttamente a livello applicativo la tabella relazionale {@code
 * USER_CLIENT_ROLE}.
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Mappato 1:1 con l'entita relazionale {@code UserClientRoleEntity}.
 *   <li>Utilizzato in {@code User.roles} per esporre i ruoli assegnati arricchiti all'utente.
 *   <li>Popolato da {@code RolesService}, {@code UserService} e {@code UserAdminService}.
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Contextualized application role assigned to a user for an OAuth2 Client")
public class UserClientRole extends GenericDTO {

  @UpperCase
  @Schema(
      description = "Identifier of the OAuth2 Client associated with the role",
      example = "LINKATUTTO-ADMIN")
  private String clientId;

  @UpperCase
  @Schema(description = "Identifier name of the assigned role", example = "ROLE_USER")
  private String role;

  @UpperCase
  @Schema(description = "Application type of the role", example = "USER")
  private String type;

  @Schema(
      description = "Textual description of the assigned role",
      example = "Standard application user role")
  private String description;

  @Schema(
      description = "List of aggregated granular permissions granted by the role",
      example = "[\"users:read\"]")
  private List<String> permissions;

  public UserClientRole(String clientId, String role) {
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
    this.role = role != null ? role.trim().toUpperCase() : null;
  }
}
