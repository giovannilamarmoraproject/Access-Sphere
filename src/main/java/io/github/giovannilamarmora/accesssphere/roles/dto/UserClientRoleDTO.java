package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * DTO che rappresenta un ruolo contestualizzato assegnato a un utente per uno specifico Client OAuth2,
 * corredato di tipologia applicativa e lista di permessi granulari aggregati.
 */
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserClientRoleDTO extends GenericDTO {

  private String clientId;
  private String role;
  private String type;
  private String description;
  private List<String> permissions;

  public UserClientRoleDTO(String clientId, String role) {
    this.clientId = clientId;
    this.role = role;
  }

  public UserClientRoleDTO(String clientId, String role, String type, List<String> permissions) {
    this.clientId = clientId;
    this.role = role;
    this.type = type;
    this.permissions = permissions;
  }
}
