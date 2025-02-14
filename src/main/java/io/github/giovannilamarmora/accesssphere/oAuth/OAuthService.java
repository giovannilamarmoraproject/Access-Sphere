package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.GoogleAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenExchange;
import io.github.giovannilamarmora.accesssphere.utilities.Cookie;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.CookieManager;
import io.github.giovannilamarmora.utils.web.RequestManager;
import java.net.URI;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Service
@Logged
@RequiredArgsConstructor
public class OAuthService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value("${cookie-domain}")
  private String cookieDomain;

  @Autowired private ClientService clientService;
  @Autowired private AuthService authService;
  @Autowired private GoogleAuthService googleAuthService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private AccessTokenData accessTokenData;
  @Autowired private TokenService tokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirect_uri,
      String scope,
      String registration_token,
      String bearer,
      String state,
      ServerHttpResponse serverHttpResponse) {

    LOG.info(
        "🔐 Starting oAuth/2.0/authorize - clientId: {}, accessType: {}, responseType: {}",
        clientId,
        accessType,
        responseType);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);
    String finalRedirectUri = Utils.decodeURLValue(redirect_uri);

    return clientCredentialMono.map(
        clientCredential -> {
          if (StringUtils.hasText(bearer)) {
            return handleBearerToken(bearer, clientCredential);
          }

          return handleOAuthAuthorization(
              clientCredential,
              responseType,
              accessType,
              finalRedirectUri,
              scope,
              registration_token,
              serverHttpResponse);
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
  public Mono<ResponseEntity<Response>> tokenExchange(
      String bearer, TokenExchange tokenExchange, ServerWebExchange exchange) {

    LOG.info("🔄 Token Exchange process started for client: {}", tokenExchange.getClient_id());

    // Recupera le credenziali del client
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(tokenExchange.getClient_id());

    Mono<ClientCredential> clientCredentialToExchangeMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono
        .zipWith(clientCredentialToExchangeMono)
        .flatMap(
            objects -> {
              OAuthValidator.validateTokenExchange(bearer, tokenExchange, objects.getT1());
              return authService.exchangeToken(
                  tokenExchange,
                  accessTokenData,
                  objects.getT1(),
                  objects.getT2(),
                  exchange.getRequest());
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "🔄 Token Exchange process completed for client: {}",
                    tokenExchange.getClient_id()));
  }

  private ResponseEntity<?> handleBearerToken(String bearer, ClientCredential clientCredential) {
    LOG.info("✅ Bearer token found *******, starting authorization check");

    try {
      AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);
      OAuthValidator.validateUserRoles(clientCredential, accessTokenData.getRoles());
      OAuthValidator.validateClientId(
          accessTokenData.getClientId(), clientCredential.getClientId());

      AuthToken token = new AuthToken();
      token.setAccess_token(bearer.contains("Bearer") ? bearer.split("Bearer ")[1] : bearer);

      Map<String, Object> strapiToken = OAuthMapper.extractStrapiToken(accessTokenData);

      Response response =
          new Response(
              HttpStatus.OK.value(),
              "Token is valid",
              TraceUtils.getSpanID(),
              strapiToken == null
                  ? new OAuthTokenResponse(token, accessTokenData.getPayload())
                  : new OAuthTokenResponse(
                      token,
                      Mapper.readTree(strapiToken),
                      Mapper.removeField(
                          accessTokenData.getPayload(), TokenData.STRAPI_TOKEN.getToken())));

      LOG.info("✅ Authorization successful for bearer: {}", bearer);
      return ResponseEntity.ok(response);

    } catch (TokenException | OAuthException e) {
      return handleInvalidToken(clientCredential, e);
    }
  }

  private ResponseEntity<?> handleInvalidToken(ClientCredential clientCredential, Exception e) {
    LOG.warn("❌ Token validation failed: {}", e.getMessage());

    if (e instanceof OAuthException exception
        && exception.getExceptionCode().equals(ExceptionMap.ERR_OAUTH_401)) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    if (!ObjectToolkit.isNullOrEmpty(clientCredential.getRedirect_uri())
        && !ObjectToolkit.isNullOrEmpty(
            clientCredential.getRedirect_uri().get(RedirectUris.FALLBACK_URI.url()))) {

      URI fallbackLocation =
          URI.create(clientCredential.getRedirect_uri().get(RedirectUris.FALLBACK_URI.url()));

      Response response =
          new Response(
              HttpStatus.FOUND.value(),
              "Token is not valid",
              TraceUtils.getSpanID(),
              fallbackLocation);

      return ResponseEntity.status(HttpStatus.FOUND).location(fallbackLocation).body(response);
    }

    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            new Response(
                HttpStatus.UNAUTHORIZED.value(),
                "Token is not valid",
                TraceUtils.getSpanID(),
                null));
  }

  private ResponseEntity<?> handleOAuthAuthorization(
      ClientCredential clientCredential,
      String responseType,
      String accessType,
      String finalRedirectUri,
      String scope,
      String registrationToken,
      ServerHttpResponse serverHttpResponse) {

    OAuthValidator.validateClient(
        clientCredential, responseType, accessType, finalRedirectUri, scope);
    URI location =
        GrpcService.getGoogleOAuthLocation(scope, finalRedirectUri, accessType, clientCredential);

    CookieManager.setCookieInResponse(
        Cookie.COOKIE_REDIRECT_URI, finalRedirectUri, cookieDomain, serverHttpResponse);
    CookieManager.setCookieInResponse(
        Cookie.COOKIE_TOKEN, registrationToken, cookieDomain, serverHttpResponse);

    LOG.info(
        "✅ OAuth authorization completed - clientId: {}, accessType: {}, responseType: {}",
        clientCredential.getClientId(),
        accessType,
        responseType);

    return ResponseEntity.status(clientCredential.getAuthorize_redirect_status())
        .location(location)
        .build();
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
              OAuthType authType =
                  OAuthMapper.getOAuthType(clientCredential, accessTokenData, grant_type, code);
              // OAuthValidator.validateClientId(clientCredential, clientId);
              switch (authType) {
                case BEARER -> {
                  OAuthValidator.validateBasicAuth(
                      basic, grant_type, redirect_uri, clientCredential);
                  return authService.makeClassicLogin(
                      basic, redirect_uri, clientCredential, request, response);
                }
                case GOOGLE -> {
                  LOG.info("Google oAuth 2.0 Login started");
                  String redirectUri = redirect_uri;
                  if (ObjectUtils.isEmpty(redirectUri))
                    // redirectUri = CookieManager.getCookie(Cookie.COOKIE_REDIRECT_URI, request);
                    redirectUri =
                        RequestManager.getCookieOrHeaderData(Cookie.COOKIE_REDIRECT_URI, request);
                  OAuthValidator.validateOAuthGoogle(
                      clientCredential, code, scope, redirectUri, grant_type);
                  GoogleModel googleModel =
                      GrpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
                  return googleAuthService
                      .performGoogleLogin(
                          googleModel, clientCredential, redirect_uri, request, response)
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
              CookieManager.deleteCookie(Cookie.COOKIE_TOKEN, response);
              CookieManager.deleteCookie(Cookie.COOKIE_REDIRECT_URI, response);
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

    // OAuthValidator.validateClientLogout(accessTokenData, clientId);

    if (isLogoutAlreadyDone()) {
      return createLogoutSuccessResponse(clientId, redirect_uri, response);
    }

    AccessTokenData tokenData = new AccessTokenData();
    BeanUtils.copyProperties(accessTokenData, tokenData);
    OAuthValidator.validateClientId(tokenData.getClientId(), clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential ->
                processLogoutByAuthType(tokenData, clientCredential, redirect_uri, response))
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
    Response res = new Response(HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);
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
    return switch (OAuthMapper.getOAuthType(clientCredential, accessTokenData, null, null)) {
      case BEARER -> authService.logout(redirect_uri, accessTokenData, response);
      case GOOGLE -> googleAuthService.processGoogleLogout(redirect_uri, accessTokenData, response);
      default -> defaultErrorType();
    };
  }

  private Mono<ResponseEntity<?>> defaultErrorType() {
    LOG.error("Type not configured, miss match on client");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }
}
