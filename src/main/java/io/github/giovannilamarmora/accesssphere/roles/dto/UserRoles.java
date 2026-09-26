package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.generic.GenericDTO;
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
   * Ruoli contestualizzati per Client OAuth2 con tipologia applicativa e permessi granulari
   * aggregati.
   */
  @JsonAlias({"clientRoles", "client_roles"})
  private List<UserClientRole> roles;

  public List<UserClientRole> getClientRoles() {
    return this.roles;
  }

  public void setClientRoles(List<UserClientRole> clientRoles) {
    this.roles = clientRoles;
  }

  public static UserRoles fromLegacyRoles(List<String> roleNames) {
    UserRoles ur = new UserRoles();
    if (roleNames != null) {
      ur.setRoles(roleNames.stream().map(r -> new UserClientRole(null, r)).toList());
    }
    return ur;
  }
}
