package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.utilities.OpenAPI;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Logged
@Validated
@RestController
@RequestMapping("/v1/oAuth/2.0")
@Tag(name = OpenAPI.Tag.OAUTH, description = OpenAPI.Description.OAUTH)
public class OAuthControllerImpl implements OAuthController {

  @Autowired private OAuthService oAuthService;

  @Override
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirectUri,
      String scope,
      String registration_token,
      String state) {
    return oAuthService.authorize(
        responseType, accessType, clientId, redirectUri, scope, registration_token, state);
  }

  @Override
  public Mono<ResponseEntity<?>> login(
      String clientId,
      String scope,
      String code,
      String prompt,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
    return oAuthService.token(
        clientId, scope, code, prompt, includeUserInfo, includeUserData, request);
  }

  @Override
  public Mono<ResponseEntity<?>> token(
      String clientId,
      String refresh_token,
      String grant_type,
      String scope,
      String code,
      String redirectUri,
      String prompt,
      boolean includeUserInfo,
      boolean includeUserData,
      String auth,
      ServerHttpRequest request) {
    return oAuthService.tokenOAuth(
        clientId,
        refresh_token,
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
