package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1/oAuth/2.0")
@Tag(name = OpenAPI.Tag.OAUTH, description = "API to Handle App")
public class OAuthControllerImpl {

  @Autowired private OAuthService oAuthService;

  @GetMapping(value = "/authorize")
  @Operation(
      description = "API to start OAuth 2.0 authorization",
      summary = "Start OAuth 2.0 Authorization",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> authorize(
      @RequestParam(value = "response_type", required = false) String responseType,
      @RequestParam(value = "access_type") @Valid @NotNull(message = "Access Type is required")
          String accessType,
      @RequestParam(value = "client_id") @Valid @NotNull(message = "Client ID is required")
          String clientId,
      @RequestParam(value = "redirect_uri") @Valid @NotNull(message = "Redirect Uri is required")
          String redirectUri,
      @RequestParam("scope") @Valid @NotNull(message = "Scopes are required") String scope,
      @RequestParam(value = "registration_token", required = false) String registration_token,
      @RequestParam(value = "state", required = false) String state) {
    return oAuthService.authorize(
        accessType, clientId, redirectUri, scope, registration_token, state);
  }

  @GetMapping(value = "/login/{client_id}", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      description = "API to perform OAuth 2.0 login",
      summary = "Perform OAuth 2.0 Login",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> login(
      @PathVariable(value = "client_id") String clientId,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "prompt", required = false) String prompt,
      @RequestParam(value = "include_user_info", required = false, defaultValue = "true")
          boolean includeUserInfo,
      @RequestParam(value = "include_user_data", required = false, defaultValue = "true")
          boolean includeUserData,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(description = "Authorization Basic")
          String basic,
      ServerHttpRequest request) {
    return oAuthService.token(
        clientId, scope, code, prompt, includeUserInfo, includeUserData, basic, request);
  }

  @GetMapping(value = "/token", produces = MediaType.APPLICATION_JSON_VALUE)
  @Operation(
      description = "API to perform OAuth 2.0 login",
      summary = "Perform OAuth 2.0 Login",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> token(
      @RequestParam(value = "client_id") String clientId,
      @RequestParam(value = "grant_type") String grant_type,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "redirect_uri", required = false) String redirectUri,
      @RequestParam(value = "prompt", required = false) String prompt,
      @RequestParam(value = "include_user_info", required = false, defaultValue = "false")
          boolean includeUserInfo,
      @RequestParam(value = "include_user_data", required = false, defaultValue = "false")
          boolean includeUserData,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(description = "Authorization Basic")
          String auth,
      ServerHttpRequest request) {
    return oAuthService.tokenOAuth(
        clientId,
        grant_type,
        scope,
        code,
        prompt,
        redirectUri,
        includeUserInfo,
        includeUserData,
        auth,
        request);
  }
}
