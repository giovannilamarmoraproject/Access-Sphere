package io.github.giovannilamarmora.accesssphere.roles.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.utils.generic.GenericEntity;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Entità relazionale enterprise che definisce un Ruolo applicativo all'interno del catalogo di un Client OAuth2.
 * Sostituisce la serializzazione JSON nei client memorizzando ciascun ruolo applicativo nella tabella dedicata {@code ROLE_DEFINITIONS}.
 * I permessi granulari associati a ciascun ruolo sono memorizzati direttamente nella colonna {@code PERMISSIONS} tramite {@link JsonStringListConverter}.
 *
 * <p>Interconnessioni:
 * <ul>
 *   <li>Mappato per Client ID ({@code CLIENT_ID}) e nome ruolo ({@code ROLE_NAME}).</li>
 *   <li>I permessi in {@link #permissions} sono serializzati in formato JSON nella colonna {@code PERMISSIONS} della stessa tabella.</li>
 *   <li>Utilizzato da {@code RolesService} per il catalogo, da {@code ClientService} e da {@code UserService} per l'arricchimento RBAC.</li>
 * </ul>
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Entity
@Table(
    name = "ROLE_DEFINITIONS",
    indexes = {
      @Index(name = "IDX_ROLE_DEF_CLIENT", columnList = "CLIENT_ID"),
      @Index(name = "IDX_ROLE_DEF_ROLE", columnList = "ROLE_NAME"),
      @Index(name = "IDX_ROLE_DEF_CLIENT_ROLE", columnList = "CLIENT_ID, ROLE_NAME")
    },
    uniqueConstraints = {
      @UniqueConstraint(
          name = "UK_ROLE_DEF_CLIENT_ROLE",
          columnNames = {"CLIENT_ID", "ROLE_NAME"})
    })
public class RoleDefinitionEntity extends GenericEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "ID", nullable = false)
  private Long id;

  @Column(name = "CLIENT_ID", nullable = false)
  private String clientId;

  @Column(name = "ROLE_NAME", nullable = false)
  private String role;

  @Column(name = "ROLE_TYPE", nullable = false)
  private String type = AppRoleType.USER.getValue();

  @Column(name = "DESCRIPTION")
  private String description;

  @Convert(converter = JsonStringListConverter.class)
  @Lob
  @Column(name = "PERMISSIONS", columnDefinition = "TEXT")
  private List<String> permissions = new ArrayList<>();

  public RoleDefinitionEntity(
      String clientId, String role, String type, String description, List<String> permissions) {
    this.clientId = clientId != null ? clientId.trim().toUpperCase() : null;
    this.role = role != null ? role.trim().toUpperCase() : null;
    this.type =
        (type != null && !"DEFAULT".equalsIgnoreCase(type.trim()))
            ? type.trim().toUpperCase()
            : AppRoleType.USER.getValue();
    this.description = description;
    this.permissions = permissions != null ? new ArrayList<>(permissions) : new ArrayList<>();
  }
}
