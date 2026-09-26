package io.github.giovannilamarmora.accesssphere.roles.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Modello di dominio primario che rappresenta un ruolo contestualizzato assegnato a un utente
 * per uno specifico Client OAuth2, arricchito con la tipologia applicativa e la lista dei permessi
 * granulari aggregati. Mappa direttamente a livello applicativo la tabella relazionale {@code USER_CLIENT_ROLE}.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Mappato 1:1 con l'entita relazionale {@code UserClientRoleEntity}.</li>
 *   <li>Utilizzato in {@code User.roles} per esporre i ruoli assegnati arricchiti all'utente.</li>
 *   <li>Popolato da {@code RolesService}, {@code UserService} e {@code UserAdminService}.</li>
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Ruolo applicativo contestualizzato assegnato a un utente per un Client OAuth2")
public class UserClientRole extends GenericDTO {

  @Schema(description = "Identificativo del Client OAuth2 associato al ruolo", example = "LINKATUTTO-ADMIN")
  private String clientId;

  @Schema(description = "Nome identificativo del ruolo assegnato", example = "ROLE_USER")
  private String role;

  @Schema(description = "Tipologia applicativa del ruolo", example = "USER")
  private String type;

  @Schema(description = "Descrizione testuale del ruolo assegnato", example = "Ruolo standard utente applicativo")
  private String description;

  @Schema(description = "Lista dei permessi granulari aggregati concessi dal ruolo", example = "[\"users:read\"]")
  private List<String> permissions;

  public UserClientRole(String role) {
    this.role = role != null ? role.trim().toUpperCase() : null;
  }

  public UserClientRole(String clientId, String role) {
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
    this.role = role != null ? role.trim().toUpperCase() : null;
  }

  public UserClientRole(String clientId, String role, String type, List<String> permissions) {
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type = type;
    this.permissions = permissions;
  }
}
