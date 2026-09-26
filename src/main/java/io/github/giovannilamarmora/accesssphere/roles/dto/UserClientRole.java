package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @deprecated Utilizzare il modello di dominio primario {@link io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole}.
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserClientRole extends io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole {

  public UserClientRole(String role) {
    super(role);
  }

  public UserClientRole(String clientId, String role) {
    super(clientId, role);
  }

  public UserClientRole(String clientId, String role, String type, List<String> permissions) {
    super(clientId, role, type, permissions);
  }

  public UserClientRole(String clientId, String role, String type, String description, List<String> permissions) {
    super(clientId, role, type, description, permissions);
  }
}
