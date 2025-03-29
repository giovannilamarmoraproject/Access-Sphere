package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.token.dto.TokenExchange;
import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Logged
@Validated
@RestController
@RequestMapping("/v1/oAuth/2.0")
@CrossOrigin("*")
// @CrossOrigin(
//    // origins = "*",
//    // allowedHeaders = "*",
//    exposedHeaders = {
//      ExposedHeaders.LOCATION,
//      ExposedHeaders.SESSION_ID,
//      ExposedHeaders.AUTHORIZATION,
//      ExposedHeaders.TRACE_ID,
//      ExposedHeaders.SPAN_ID,
//      ExposedHeaders.PARENT_ID,
//      ExposedHeaders.REDIRECT_URI,
//      ExposedHeaders.REGISTRATION_TOKEN
//    })
@Tag(name = OpenAPI.Tag.OAUTH, description = OpenAPI.Description.OAUTH)
public class OAuthControllerImpl implements OAuthController {

  @Autowired private OAuthService oAuthService;

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirectUri,
      String scope,
      String registration_token,
      String bearer,
      String state,
      ServerHttpResponse serverHttpResponse) {
    return oAuthService.authorize(
        responseType,
        accessType,
        clientId,
        redirectUri,
        scope,
        registration_token,
        bearer,
        state,
        serverHttpResponse);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> login(
      String clientId, String scope, String code, String prompt, ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    return oAuthService.token(clientId, scope, code, prompt, exchange);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> token(
      String clientId,
      String refresh_token,
      String grant_type,
      String scope,
      String code,
      String redirectUri,
      String prompt,
      String auth,
      ServerWebExchange exchange) {
    return oAuthService.tokenOAuth(
        clientId, refresh_token, grant_type, scope, code, prompt, redirectUri, auth, exchange);
  }

  @Override
  public Mono<ResponseEntity<Response>> tokenExchange(
      String bearer, TokenExchange tokenExchange, ServerWebExchange exchange) {
    return oAuthService.tokenExchange(bearer, tokenExchange, exchange);
  }

  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.CONTROLLER)
  public Mono<ResponseEntity<?>> logout(
      String clientId, String redirectUri, String auth, ServerHttpResponse response) {
    return oAuthService.logout(clientId, redirectUri, auth, response);
  }
}
