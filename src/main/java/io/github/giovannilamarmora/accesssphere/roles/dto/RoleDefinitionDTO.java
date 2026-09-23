package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO che modella la definizione completa di un ruolo applicativo per il catalogo centralizzato /v1/roles.
 * Contiene i metadati del ruolo (clientId, nome, tipologia, permessi, descrizione) e, in fase di lettura,
 * il conteggio e la lista sintetica degli utenti a cui è attualmente assegnato.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RoleDefinitionDTO extends GenericDTO {

  @NotBlank(message = "Client ID is required")
  private String clientId;

  @NotBlank(message = "Role name is required")
  private String role;

  private String type = AppRoleType.DEFAULT.getValue();

  private String description;

  private List<String> permissions;

  private Integer assignedUsersCount;

  private List<UserSummaryDTO> assignedUsers;

  public RoleDefinitionDTO(String clientId, String role, String type, String description, List<String> permissions) {
    this.clientId = clientId;
    this.role = role;
    this.type = type != null ? type : AppRoleType.DEFAULT.getValue();
    this.description = description;
    this.permissions = permissions;
  }
}
