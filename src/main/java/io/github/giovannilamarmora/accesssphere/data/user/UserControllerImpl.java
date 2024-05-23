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
@RequestMapping("/v1")
@Tag(name = OpenAPI.Tag.USERS, description = "API to manage users")
public class UserControllerImpl {

  @Autowired private UserService userService;

  @GetMapping("/userInfo")
  @Operation(
      description = "Obtaining the Info of the current User",
      summary = "User Info",
      tags = OpenAPI.Tag.USERS)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userInfo(
      @RequestParam(value = "include_user_data", required = false, defaultValue = "false")
          @Schema(
              description = OpenAPI.Params.Description.INCLUDE_USER_DATA,
              example = OpenAPI.Params.Example.INCLUDE_USER_DATA)
          boolean includeUserData,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(
              description = OpenAPI.Params.Description.BEARER,
              example = OpenAPI.Params.Example.BEARER)
          String bearer) {
    return userService.userInfo(bearer, includeUserData);
  }

  @PostMapping("/users/register")
  @Operation(
      description = "Register a new user",
      summary = "User Registration",
      tags = OpenAPI.Tag.USERS)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> registerUser(
      @RequestBody @Valid @NotNull User user,
      @RequestParam(value = "client_id") String clientId,
      @RequestParam(value = "registration_token")
          @NotNull(message = "Registration Token is Required")
          String registration_token) {
    return userService.register(user, clientId, registration_token);
  }
}
