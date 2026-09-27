package io.github.giovannilamarmora.accesssphere.roles.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.ArrayList;
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
 *   <li>Mappato 1:1 con l'entita relazionale {@code RoleDefinitionEntity}.
 *   <li>Utilizzato da {@code OAuth2Client.appRoles} per la configurazione dei ruoli per singolo
 *       client.
 *   <li>Utilizzato da {@code RolesController} e {@code RolesService} per il catalogo centralizzato
 *       {@code /v1/roles}.
 *   <li>Supporta la deserializzazione resiliente da stringa singola o da oggetto JSON tramite
 *       {@link RoleDefinitionDeserializer}.
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = RoleDefinitionDeserializer.class)
@Schema(description = "Complete definition of an application role for an OAuth2 Client")
public class RoleDefinition extends GenericDTO {

  @Schema(description = "Numeric identifier of the role in the database", example = "1")
  private Long id;

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

  public RoleDefinition(String clientId, List<String> permissions) {
    this.clientId = clientId;
    this.permissions = permissions;
  }

  /**
   * Costruttore di convenienza dal solo nome ruolo.
   *
   * @param role Nome identificativo del ruolo.
   */
  public RoleDefinition(String role) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type = AppRoleType.USER.getValue();
  }

  /**
   * Costruttore con nome ruolo e tipologia applicativa.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   */
  public RoleDefinition(String role, String type) {
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !type.isBlank())
            ? ("DEFAULT".equalsIgnoreCase(type.trim())
                ? AppRoleType.USER.getValue()
                : type.trim().toUpperCase())
            : AppRoleType.USER.getValue();
  }

  /**
   * Costruttore con nome, tipo e descrizione.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione testuale.
   */
  public RoleDefinition(String role, String type, String description) {
    this(role, type);
    this.description = description;
  }

  /**
   * Costruttore con nome, tipo, descrizione e permessi.
   *
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione testuale.
   * @param permissions Permessi granulari associati.
   */
  public RoleDefinition(String role, String type, String description, List<String> permissions) {
    this(role, type, description);
    this.permissions = permissions != null ? new ArrayList<>(permissions) : new ArrayList<>();
  }

  /**
   * Costruttore completo comprensivo di clientId.
   *
   * @param clientId Identificativo Client OAuth2.
   * @param role Nome del ruolo.
   * @param type Tipologia applicativa.
   * @param description Descrizione.
   * @param permissions Permessi granulari.
   */
  public RoleDefinition(
      String clientId, String role, String type, String description, List<String> permissions) {
    this(role, type, description, permissions);
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
  }

  /** Costruttore completo comprensivo di clientId e conteggio utenti assegnati. */
  public RoleDefinition(
      String clientId,
      String role,
      String type,
      String description,
      List<String> permissions,
      Integer assignedUsersCount,
      List<UserSummaryDTO> assignedUsers) {
    this(clientId, role, type, description, permissions);
    this.assignedUsersCount = assignedUsersCount;
    this.assignedUsers = assignedUsers;
  }

  /** Costruttore con id, ruolo, tipo e descrizione. */
  public RoleDefinition(Long id, String role, String type, String description) {
    this(role, type, description);
    this.id = id;
  }

  /**
   * Costruttore con id, ruolo, tipo, descrizione e permessi.
   *
   * @param id Identificativo database.
   * @param role Nome identificativo del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione testuale.
   * @param permissions Permessi granulari associati.
   */
  public RoleDefinition(
      Long id, String role, String type, String description, List<String> permissions) {
    this(role, type, description, permissions);
    this.id = id;
  }

  /**
   * Verifica se il ruolo corrente e marcato con la tipologia standard USER.
   *
   * @return true se il tipo corrisponde a USER, false altrimenti.
   */
  public boolean isUser() {
    return AppRoleType.USER.getValue().equalsIgnoreCase(this.type);
  }

  /**
   * Verifica se il nome del ruolo corrisponde a quello specificato (case-insensitive).
   *
   * @param targetRole Nome del ruolo da confrontare.
   * @return true se coincidono, false altrimenti.
   */
  public boolean matches(String targetRole) {
    return this.role != null && this.role.equalsIgnoreCase(targetRole);
  }

  /**
   * Factory method statico per istanziare un RoleDefinition dal solo nome.
   *
   * @param role Nome del ruolo.
   * @return Nuova istanza di {@link RoleDefinition}.
   */
  public static RoleDefinition of(String role) {
    return new RoleDefinition(role);
  }

  /**
   * Factory method statico completo.
   *
   * @param role Nome del ruolo.
   * @param type Tipologia del ruolo.
   * @param description Descrizione del ruolo.
   * @return Nuova istanza di {@link RoleDefinition}.
   */
  public static RoleDefinition of(String role, String type, String description) {
    return new RoleDefinition(role, type, description);
  }
}
