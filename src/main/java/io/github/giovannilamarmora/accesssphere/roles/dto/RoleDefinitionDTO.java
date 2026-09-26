package io.github.giovannilamarmora.accesssphere.roles.dto;

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
public class RoleDefinitionDTO extends RoleDefinition {

  public RoleDefinitionDTO(String clientId, String role, String type, String description, List<String> permissions) {
    super(clientId, role, type, description, permissions);
  }

  public RoleDefinitionDTO(
      String clientId,
      String role,
      String type,
      String description,
      List<String> permissions,
      Integer assignedUsersCount,
      List<UserSummaryDTO> assignedUsers) {
    super(clientId, role, type, description, permissions, assignedUsersCount, assignedUsers);
  }
}
