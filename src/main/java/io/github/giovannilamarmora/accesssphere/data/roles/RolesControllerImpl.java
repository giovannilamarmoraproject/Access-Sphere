package io.github.giovannilamarmora.accesssphere.data.roles;

import io.github.giovannilamarmora.accesssphere.data.roles.dto.UserRoles;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1")
@CrossOrigin("*")
@Tag(name = OpenAPI.Tag.ROLES, description = "API to manage user roles")
public class RolesControllerImpl implements RolesController {

  @Autowired private RolesService rolesService;

  @Override
  public Mono<ResponseEntity<Response>> changeRoles(
      String bearer, String identifier, UserRoles roles) {
    return rolesService.changeRoles(identifier, roles);
  }
}
