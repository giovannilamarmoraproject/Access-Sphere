package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.*;

/**
 * Modello di dominio primario che rappresenta la definizione completa di un Ruolo applicativo
 * associato a un Client OAuth2. Unifica e sostituisce {@code AppRole} e {@code RoleDefinitionDTO},
 * mappando direttamente la tabella relazionale {@code ROLE_DEFINITIONS}.
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Utilizzato da {@code OAuth2Client.appRoles} per la configurazione dei ruoli per singolo
 *       client.
 *   <li>Utilizzato da {@code RolesController} e {@code RolesService} per il catalogo centralizzato
 *       {@code /v1/roles}.
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Complete definition of an application role for an OAuth2 Client")
public class RoleDefinitionResponse extends GenericDTO {

  @NotBlank(message = "Client ID is required")
  @Schema(
      description = "Identifier of the OAuth2 Client that owns the role",
      example = "CLIENT-SAMPLE-001")
  private String clientId;

  @NotBlank(message = "Role name is required")
  @JsonAlias({"name", "roleName", "value"})
  @Schema(description = "Unique identifier name of the role for the client", example = "ROLE_USER")
  private String role;

  @Schema(description = "Role type (USER, ADMIN, TECHNICAL, MANAGER, etc.)", example = "USER")
  private String type;

  @Schema(
      description = "Textual description of the role's responsibilities",
      example = "Standard application user role")
  private String description;

  @Schema(
      description =
          "List of granular permissions associated with the role (e.g. [\"users:read\", \"users:write\"])",
      example = "[\"users:read\", \"users:write\"]")
  private List<String> permissions;

  @Schema(description = "Count of users currently assigned to this role", example = "5")
  private Integer assignedUsersCount;

  @Schema(description = "Summary list of users currently assigned to this role")
  private List<UserSummaryDTO> assignedUsers;
}
