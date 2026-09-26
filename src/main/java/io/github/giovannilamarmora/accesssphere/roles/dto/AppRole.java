package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import java.util.List;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @deprecated Utilizzare il modello di dominio primario {@link RoleDefinition}.
 */
@Deprecated(since = "2.0.0", forRemoval = false)
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AppRole extends RoleDefinition {

  public AppRole(String role) {
    super(role);
  }

  public AppRole(String role, String type) {
    super(role, type);
  }

  public AppRole(String role, String type, String description) {
    super(role, type, description);
  }

  public AppRole(String role, String type, String description, List<String> permissions) {
    super(role, type, description, permissions);
  }

  public AppRole(Long id, String role, String type, String description) {
    super(id, role, type, description);
  }

  public static AppRole of(String role) {
    return new AppRole(role);
  }

  public static AppRole of(String role, String type, String description) {
    return new AppRole(role, type, description);
  }
}
