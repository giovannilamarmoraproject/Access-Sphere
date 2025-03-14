package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.data.user.dto.ChangePassword;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
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
@CrossOrigin("*")
@Tag(name = OpenAPI.Tag.USERS, description = "API to manage users")
public class UserControllerImpl implements UserController {

  @Autowired private UserService userService;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    return userService.userInfo(bearer, request);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> profile(String bearer, ServerHttpRequest request) {
    return userService.profile(bearer, request);
  }

  @Override
  public Mono<ResponseEntity<Response>> userList(String bearer, ServerHttpRequest request) {
    return userService.getUsers();
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> registerUser(
      String bearer,
      User user,
      String clientId,
      String registration_token,
      Boolean assignNewClient) {
    return userService.register(bearer, user, clientId, registration_token, assignNewClient);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> updateUser(
      User user, String bearer, ServerHttpRequest request) {
    return userService.updateUser(user, bearer, request);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePasswordRequest(
      ChangePassword changePassword, String locale, boolean sendEmail) {
    return userService.changePasswordRequest(changePassword, locale, sendEmail);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<Response>> changePassword(ChangePassword changePassword) {
    return userService.changePassword(changePassword);
  }
}
