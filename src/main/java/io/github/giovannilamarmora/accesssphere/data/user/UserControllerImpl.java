package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1/users")
@Tag(name = OpenAPI.Tag.USERS, description = "API to manage users")
public class UserControllerImpl {

  @Autowired private UserService userService;

  @PostMapping("/register")
  @Operation(
      description = "Register a new user",
      summary = "User Registration",
      tags = OpenAPI.Tag.USERS)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> registerUser(
      @RequestBody @Valid @NotNull User user,
      @RequestParam(value = "client_id") @NotNull(message = "Client ID is Required")
          String clientId,
      @RequestParam(value = "registration_token")
          @NotNull(message = "Registration Token is Required")
          String registration_token) {
    return userService.register(user, clientId, registration_token);
  }
}
