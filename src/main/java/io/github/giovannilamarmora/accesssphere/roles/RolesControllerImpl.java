package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.ROLES, description = OpenAPI.Description.ROLES)
public class RolesControllerImpl implements RolesController {

  @Autowired private RolesService rolesService;

  @Override
  public Mono<ResponseEntity<Response>> changeRoles(
      String bearer, String identifier, List<UserClientRole> roles) {
    return rolesService.changeRoles(identifier, roles);
  }

  @Override
  public Mono<ResponseEntity<Response>> assignClientRole(
      String bearer, String identifier, String clientId, String role) {
    return rolesService.assignClientRole(identifier, clientId, role);
  }

  @Override
  public Mono<ResponseEntity<Response>> revokeClientRole(
      String bearer, String identifier, String clientId, String role) {
    return rolesService.revokeClientRole(identifier, clientId, role);
  }

  @Override
  public Mono<ResponseEntity<Response>> getRolesCatalog(String bearer, String clientId) {
    return rolesService.getRolesCatalog(clientId);
  }

  @Override
  public Mono<ResponseEntity<Response>> getUsersWithRole(
      String bearer, String role, String clientId) {
    return rolesService.getUsersWithRole(clientId, role);
  }

  @Override
  public Mono<ResponseEntity<Response>> createRole(String bearer, RoleDefinition dto) {
    return rolesService.createRole(dto);
  }

  @Override
  public Mono<ResponseEntity<Response>> updateRole(
      String bearer, String role, String clientId, RoleDefinition dto) {
    return rolesService.updateRole(clientId, role, dto);
  }

  @Override
  public Mono<ResponseEntity<Response>> deleteRole(String bearer, String role, String clientId) {
    return rolesService.deleteRole(clientId, role);
  }
}
