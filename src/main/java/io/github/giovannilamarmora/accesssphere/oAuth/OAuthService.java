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
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
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
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.server.ServerWebExchange;
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
  @Autowired private AccessTokenData accessTokenData;

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
        "\uD83D\uDD10 Starting endpoint oAuth/2.0/authorize with client id: {}, access_type: {}, response_type: {} and registration_token: {}",
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
              "\uD83D\uDD10 Completed endpoint oAuth/2.0/authorize with client id: {}, access_type: {}, response_type: {} and registration_token: {}",
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
      String clientId, String scope, String code, String prompt, ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    return getToken(
        clientId,
        GrantType.AUTHORIZATION_CODE.type(),
        scope,
        code,
        prompt,
        null,
        null,
        request,
        response);
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
      ServerWebExchange exchange) {
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    redirect_uri = Utils.decodeURLValue(redirect_uri);
    switch (GrantType.fromType(grant_type)) {
      case AUTHORIZATION_CODE, PASSWORD -> {
        return getToken(
            clientId, grant_type, scope, code, prompt, redirect_uri, basic, request, response);
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
      ServerHttpRequest request,
      ServerHttpResponse response) {
    LOG.info(
        "\uD83D\uDDDD\uFE0F Starting oAuth/2.0/token endpoint with client_id={}, grant_type={}",
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
            responseEntity -> {
              CookieManager.deleteCookie(AppConfig.COOKIE_TOKEN, response);
              CookieManager.deleteCookie(AppConfig.COOKIE_REDIRECT_URI, response);
              LOG.info(
                  "\uD83D\uDDDD\uFE0F Ending oAuth/2.0/token endpoint with client_id={}, grant_type={}",
                  clientId,
                  grant_type);
            });
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

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> logout(
      String clientId, String redirect_uri, String bearer, ServerHttpResponse response) {
    LOG.info("\uD83D\uDDDD\uFE0F Starting oAuth/2.0/logout endpoint with client_id={}", clientId);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    OAuthValidator.validateClientLogout(accessTokenData, clientId);

    if (isLogoutAlreadyDone()) {
      return createLogoutSuccessResponse(clientId, redirect_uri, response);
    }

    return clientCredentialMono
        .flatMap(
            clientCredential ->
                processLogoutByAuthType(accessTokenData, clientCredential, redirect_uri, response))
        .doOnSuccess(
            responseEntity ->
                LOG.info(
                    "\uD83D\uDDDD\uFE0F Ending oAuth/2.0/logout endpoint with client_id={}",
                    clientId));
  }

  private boolean isLogoutAlreadyDone() {
    return ObjectUtils.isEmpty(accessTokenData.getClientId());
  }

  private Mono<ResponseEntity<?>> createLogoutSuccessResponse(
      String clientId, String redirect_uri, ServerHttpResponse response) {
    LOG.warn("Some data are empty, logout already done");
    SessionID.invalidateSessionID(response);
    String message = "Logout Successfully!";
    Response res =
        new Response(HttpStatus.OK.value(), message, CorrelationIdUtils.getCorrelationId(), null);
    ResponseEntity<Response> responseResponseEntity = ResponseEntity.ok(res);
    if (!ObjectUtils.isEmpty(redirect_uri)) {
      responseResponseEntity =
          ResponseEntity.status(HttpStatus.OK).location(URI.create(redirect_uri)).body(res);
    }
    LOG.info("\uD83D\uDDDD\uFE0F Ending /oAuth/logout endpoint with client_id={}", clientId);
    return Mono.just(responseResponseEntity);
  }

  private Mono<ResponseEntity<?>> processLogoutByAuthType(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      String redirect_uri,
      ServerHttpResponse response) {
    switch (clientCredential.getAuthType()) {
      case BEARER:
        return authService.logout(redirect_uri, accessTokenData, response);
      case GOOGLE:
        return googleAuthService.processGoogleLogout(redirect_uri, accessTokenData, response);
      default:
        return defaultErrorType();
    }
  }

  private Mono<ResponseEntity<?>> defaultErrorType() {
    LOG.error("Type not configured, miss match on client");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }
}
