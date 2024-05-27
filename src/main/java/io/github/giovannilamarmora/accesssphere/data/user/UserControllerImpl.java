package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.USERS, description = "API to manage users")
public class UserControllerImpl implements UserController {

  @Autowired private UserService userService;

  @Override
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    return userService.userInfo(bearer, request);
  }

  @Override
  public Mono<ResponseEntity<Response>> registerUser(
      User user, String clientId, String registration_token) {
    return userService.register(user, clientId, registration_token);
  }

  public Mono<ResponseEntity<Response>> updateUser(
      User user, String clientId, String registration_token) {
    return userService.register(user, clientId, registration_token);
  }
}
