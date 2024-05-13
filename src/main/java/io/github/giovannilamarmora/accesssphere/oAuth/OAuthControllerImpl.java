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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@RestController
@RequestMapping("/v1/oAuth/2.0")
@Tag(name = OpenAPI.Tag.OAUTH, description = "API to Handle App")
public class OAuthControllerImpl {

  @Autowired private OAuthService oAuthService;

  @GetMapping("/authorize")
  @Operation(
      description = "API to start OAuth 2.0 authorization",
      summary = "Start OAuth 2.0 Authorization",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> authorize(
      @RequestParam(value = "response_type", required = false) String responseType,
      @RequestParam(value = "access_type") @NotNull(message = "Access Type is required")
          String accessType,
      // TODO: [Validazione] Testa validazione NotNull
      @RequestParam(value = "client_id") @NotNull(message = "Client ID is required")
          String clientId,
      @RequestParam(value = "redirect_uri") @NotNull(message = "Redirect Uri is required")
          String redirectUri,
      @RequestParam("scope") @NotNull(message = "Scopes are required") String scope,
      @RequestParam(value = "state", required = false) String state) {
    return oAuthService.authorize(accessType, clientId, redirectUri, scope, state);
  }

  @GetMapping("/login/{client_id}")
  @Operation(
      description = "API to perform OAuth 2.0 login",
      summary = "Perform OAuth 2.0 Login",
      tags = OpenAPI.Tag.OAUTH)
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<?> login(
      @PathVariable(value = "client_id") String clientId,
      @RequestParam(value = "scope", required = false) String scope,
      @RequestParam(value = "code", required = false) String code,
      @RequestParam(value = "prompt", required = false) String prompt,
      @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
          @Valid
          @Schema(description = "Authorization Basic")
          String basic) {
    return oAuthService.login(clientId, scope, code, prompt, basic);
  }
}
