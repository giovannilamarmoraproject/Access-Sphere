package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserRoles extends GenericDTO {

  /**
   * @deprecated TODO: Scheduled for removal in future major release. Use clientRoles instead.
   * Lista legacy di ruoli flat conservata per il 100% di retrocompatibilita con i client e consumer esistenti.
   */
  @Deprecated
  private List<String> roles;

  /**
   * Ruoli contestualizzati per Client OAuth2 con tipologia applicativa e permessi granulari aggregati.
   */
  private List<UserClientRoleDTO> clientRoles;

  public UserRoles(List<String> roles) {
    this.roles = roles;
  }
}
