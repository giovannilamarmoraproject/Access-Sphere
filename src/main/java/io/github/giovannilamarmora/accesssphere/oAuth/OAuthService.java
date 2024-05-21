package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  public static final String COOKIE_TOKEN = "REGISTRATION-TOKEN";
  public static final String COOKIE_REDIRECT_URI = "REDIRECT-URI";

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Autowired private TokenService tokenService;
  @Autowired private ClientService clientService;
  @Autowired private AuthService authService;
  @Autowired private GrpcService grpcService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirectUri,
      String scope,
      String registration_token,
      String state) {
    LOG.info("Starting endpoint authorize with client id: {}", clientId);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.map(
        clientCredential -> {
          OAuthValidator.validateClient(
              clientCredential, responseType, accessType, redirectUri, scope);
          URI location =
              grpcService.getGoogleOAuthLocation(scope, redirectUri, accessType, clientCredential);
          HttpHeaders headers = Utils.setCookieInResponse(COOKIE_REDIRECT_URI, redirectUri);
          headers.addAll(
              !ObjectUtils.isEmpty(registration_token)
                  ? Utils.setCookieInResponse(COOKIE_TOKEN, registration_token)
                  : new HttpHeaders());
          LOG.info("Completed endpoint authorize with client id: {}", clientId);
          return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT)
              .location(location)
              .headers(headers)
              .build();
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> token(
      String clientId,
      String scope,
      String code,
      String prompt,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
    return getToken(
        clientId,
        GrantType.AUTHORIZATION_CODE.type(),
        scope,
        code,
        prompt,
        null,
        includeUserInfo,
        includeUserData,
        null,
        request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> tokenOAuth(
      String clientId,
      String grant_type,
      String scope,
      String code,
      String prompt,
      String redirect_uri,
      boolean includeUserInfo,
      boolean includeUserData,
      String basic,
      ServerHttpRequest request) {
    switch (GrantType.fromType(grant_type)) {
      case AUTHORIZATION_CODE, PASSWORD -> {
        return getToken(
            clientId,
            grant_type,
            scope,
            code,
            prompt,
            redirect_uri,
            includeUserInfo,
            includeUserData,
            basic,
            request);
      }
      case REFRESH_TOKEN -> {
        return refreshToken(clientId, grant_type, scope, includeUserData, includeUserData, request);
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
      boolean includeUserInfo,
      boolean includeUserData,
      String basic,
      ServerHttpRequest request) {
    LOG.info(
        "Starting /token endpoint for host={} client_id={}, grant_type={}",
        ObjectUtils.isEmpty(request.getRemoteAddress())
            ? null
            : request.getRemoteAddress().getHostName(),
        clientId,
        grant_type);
    OAuthValidator.validateOAuthToken(clientId, grant_type);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          switch (clientCredential.getAuthType()) {
            case BEARER -> {
              OAuthValidator.validateBasicAuth(basic, grant_type);
              return authService.makeClassicLogin(
                  basic, clientCredential, includeUserInfo, includeUserData, request);
            }
            case GOOGLE -> {
              LOG.info("Google oAuth 2.0 Login started");
              String redirectUri = redirect_uri;
              if (ObjectUtils.isEmpty(redirectUri))
                redirectUri = Utils.getCookie(COOKIE_REDIRECT_URI, request);
              OAuthValidator.validateOAuthGoogle(
                  clientCredential, code, scope, redirectUri, grant_type);
              GoogleModel googleModel =
                  grpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
              return authService
                  .performGoogleLogin(
                      googleModel, clientCredential, includeUserInfo, includeUserData, request)
                  .doOnSuccess(responseResponseEntity -> LOG.info("Google oAuth 2.0 Login ended"));
            }
            default -> {
              LOG.error("Type miss match on client");
              return Mono.error(
                  new OAuthException(
                      ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
            }
          }
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> refreshToken(
      String clientId,
      String grant_type,
      String scope,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
    LOG.info(
        "Starting refresh_token endpoint for host={} client_id={}, grant_type={}",
        ObjectUtils.isEmpty(request.getRemoteAddress())
            ? null
            : request.getRemoteAddress().getHostName(),
        clientId,
        grant_type);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);
    return Mono.empty();
  }
}
