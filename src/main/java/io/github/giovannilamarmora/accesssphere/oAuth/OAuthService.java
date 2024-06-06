package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.config.AppConfig;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.GoogleAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@Logged
@RequiredArgsConstructor
public class OAuthService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private ClientService clientService;
  @Autowired private AuthService authService;
  @Autowired private GoogleAuthService googleAuthService;
  @Autowired private AccessTokenService accessTokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirect_uri,
      String scope,
      String registration_token,
      String state) {
    LOG.info(
        "\uD83D\uDD10 Starting endpoint authorize with client id: {}, access_type: {}, response_type: {} and registration_token: {}",
        clientId,
        accessType,
        responseType,
        registration_token);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    String finalRedirect_uri = Utils.decodeURLValue(redirect_uri);
    return clientCredentialMono.map(
        clientCredential -> {
          OAuthValidator.validateClient(
              clientCredential, responseType, accessType, finalRedirect_uri, scope);
          URI location =
              GrpcService.getGoogleOAuthLocation(
                  scope, finalRedirect_uri, accessType, clientCredential);
          HttpHeaders headers =
              CookieManager.setCookieInResponse(AppConfig.COOKIE_REDIRECT_URI, finalRedirect_uri);
          headers.addAll(
              !ObjectUtils.isEmpty(registration_token)
                  ? CookieManager.setCookieInResponse(AppConfig.COOKIE_TOKEN, registration_token)
                  : new HttpHeaders());
          LOG.info(
              "\uD83D\uDD10 Completed endpoint authorize with client id: {}, access_type: {}, response_type: {} and registration_token: {}",
              clientId,
              accessType,
              responseType,
              registration_token);
          return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
              .location(location)
              .headers(headers)
              .build();
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> token(
      String clientId, String scope, String code, String prompt, ServerHttpRequest request) {
    return getToken(
        clientId, GrantType.AUTHORIZATION_CODE.type(), scope, code, prompt, null, null, request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> tokenOAuth(
      String clientId,
      String refresh_token,
      String grant_type,
      String scope,
      String code,
      String prompt,
      String redirect_uri,
      String basic,
      ServerHttpRequest request) {
    redirect_uri = Utils.decodeURLValue(redirect_uri);
    switch (GrantType.fromType(grant_type)) {
      case AUTHORIZATION_CODE, PASSWORD -> {
        return getToken(clientId, grant_type, scope, code, prompt, redirect_uri, basic, request);
      }
      case REFRESH_TOKEN -> {
        return refreshToken(clientId, refresh_token, grant_type, scope, request);
      }
      default -> {
        LOG.error("Invalid grant_type for {}", grant_type);
        throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid o not supported grant_type!");
      }
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> getToken(
      String clientId,
      String grant_type,
      String scope,
      String code,
      String prompt,
      String redirect_uri,
      String basic,
      ServerHttpRequest request) {
    LOG.info(
        "\uD83D\uDDDD\uFE0F Starting /token endpoint with client_id={}, grant_type={}",
        clientId,
        grant_type);
    OAuthValidator.validateOAuthToken(clientId, grant_type);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              switch (clientCredential.getAuthType()) {
                case BEARER -> {
                  OAuthValidator.validateBasicAuth(basic, grant_type);
                  return authService.makeClassicLogin(basic, clientCredential, request);
                }
                case GOOGLE -> {
                  LOG.info("Google oAuth 2.0 Login started");
                  String redirectUri = redirect_uri;
                  if (ObjectUtils.isEmpty(redirectUri))
                    redirectUri = CookieManager.getCookie(AppConfig.COOKIE_REDIRECT_URI, request);
                  OAuthValidator.validateOAuthGoogle(
                      clientCredential, code, scope, redirectUri, grant_type);
                  GoogleModel googleModel =
                      GrpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
                  return googleAuthService
                      .performGoogleLogin(googleModel, clientCredential, request)
                      .doOnSuccess(
                          responseResponseEntity -> LOG.info("Google oAuth 2.0 Login ended"));
                }
                default -> {
                  return defaultErrorType();
                }
              }
            })
        .doOnSuccess(
            responseEntity ->
                LOG.info(
                    "\uD83D\uDDDD\uFE0F Ending /token endpoint with client_id={}, grant_type={}",
                    clientId,
                    grant_type));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> refreshToken(
      String clientId,
      String refresh_token,
      String grant_type,
      String scope,
      ServerHttpRequest request) {
    LOG.info(
        "\uD83D\uDD03 Starting refresh_token endpoint with client_id={}, grant_type={}",
        clientId,
        grant_type);
    OAuthValidator.validateRefreshToken(refresh_token, grant_type);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              AccessTokenData accessTokenData = accessTokenService.getByRefreshToken(refresh_token);
              OAuthValidator.validateRefreshTokenData(accessTokenData, clientCredential);
              switch (clientCredential.getAuthType()) {
                case BEARER -> {
                  return authService.refreshToken(accessTokenData, clientCredential, request);
                }
                case GOOGLE -> {
                  return googleAuthService.refreshGoogleToken(
                      accessTokenData, clientCredential, request);
                }
                default -> {
                  return defaultErrorType();
                }
              }
            })
        .doOnSuccess(
            responseEntity ->
                LOG.info(
                    "\uD83D\uDD03 Ending refresh_token endpoint with client_id={}, grant_type={}",
                    clientId,
                    grant_type));
  }

  private Mono<ResponseEntity<?>> defaultErrorType() {
    LOG.error("Type not configured, miss match on client");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }
}
