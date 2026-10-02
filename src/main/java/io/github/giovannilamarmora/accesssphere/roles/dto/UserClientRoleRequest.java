package io.github.giovannilamarmora.accesssphere.roles.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleHolder;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserClientRoleRequest implements RoleHolder {

  @NotBlank(message = "The field 'clientId' is required and it cannot be null or blank")
  private String clientId;

  @NotBlank(message = "The field 'role' is required and it cannot be null or blank")
  private String role;
}
