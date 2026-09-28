package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.client.OAuth2ClientDomainService;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.AuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.auth.GoogleAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.OAuth2AuthorizationService;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.TokenExchange;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.utilities.Cookie;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.accesssphere.utilities.URLUtils;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.CookieManager;
import io.github.giovannilamarmora.utils.web.RequestManager;
import java.net.URI;
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

  @Value("${cookie-domain:}")
  private String cookieDomain;

  @Autowired private OAuth2ClientDomainService oAuth2ClientDomainService;
  @Autowired private AuthService authService;
  @Autowired private GoogleAuthService googleAuthService;
  @Autowired private OAuth2AuthorizationService oAuth2AuthorizationService;
  @Autowired private OAuth2Authorization oAuth2Authorization;
  @Autowired private UserService userService;

  /**
   * Gestisce l'endpoint di autorizzazione OAuth 2.0 (/oauth2/authorize). Determina se la richiesta
   * richiede l'emissione immediata di un Bearer token (nel caso di token già presente o
   * response_type=token) oppure se reindirizzare l'utente verso il flusso di autenticazione
   * standard o identity provider federato (Google).
   *
   * @param responseType Tipo di risposta richiesta (es. "code" o "token").
   * @param accessType Tipo di accesso richiesto (es. "offline" o "online").
   * @param clientId Identificatore univoco del client OAuth2 registrato.
   * @param redirect_uri URI di reindirizzamento post-autorizzazione.
   * @param scope Scope richiesti per l'autorizzazione.
   * @param registration_token Token di registrazione opzionale.
   * @param bearer Token di autorizzazione bearer opzionale fornito nell'header.
   * @param state Valore di stato anti-CSRF.
   * @param exchange Contesto dello scambio HTTP server reattivo.
   * @return Mono contenente la risposta HTTP con reindirizzamento o payload token.
   */
  public Mono<ResponseEntity<?>> authorize(
      String responseType,
      String accessType,
      String clientId,
      String redirect_uri,
      String scope,
      String registration_token,
      String bearer,
      String state,
      String prompt,
      ServerWebExchange exchange) {

    LOG.info(
        "🔐 [OAUTH AUTHORIZE] Starting authorization flow - clientId: {}, accessType: {}, responseType: {}, state: {}, prompt: {}",
        clientId,
        accessType,
        responseType,
        state,
        prompt);

    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(clientId);
    String finalRedirectUri = URLUtils.decodeURLValue(redirect_uri);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          if (StringUtils.hasText(bearer) || responseType.equalsIgnoreCase("token")) {
            LOG.info(
                "🛡️ [OAUTH BEARER] Bearer token provided or responseType=token, validating bearer token for clientId: {}",
                clientId);
            OAuthValidator.validateResponseType("token", responseType);
            return handleBearerToken(bearer, clientCredential, exchange, state);
          }

          LOG.info(
              "🌐 [OAUTH AUTHORIZE] Initiating standard OAuth authorization redirect for clientId: {}, redirectUri: {}",
              clientId,
              finalRedirectUri);
          return Mono.just(
              handleOAuthAuthorization(
                  clientCredential,
                  responseType,
                  accessType,
                  finalRedirectUri,
                  scope,
                  registration_token,
                  state,
                  prompt,
                  exchange));
        });
  }

  /**
   * Endpoint universale di emissione token OAuth 2.0 con supporto per molteplici grant types:
   * authorization_code, password e refresh_token.
   *
   * @param clientId Identificatore del client.
   * @param refresh_token Token di refresh per grant_type refresh_token.
   * @param grant_type Tipo di grant richiesto (es. authorization_code, password, refresh_token).
   * @param scope Scope richiesti.
   * @param code Codice di autorizzazione.
   * @param prompt Parametro di prompt.
   * @param redirect_uri URI di reindirizzamento.
   * @param basic Stringa di credenziali HTTP Basic (per grant type password / bearer).
   * @param exchange Contesto dello scambio HTTP.
   * @return Mono con la risposta del token emesso o errore.
   */
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
    LOG.info(
        "🗝️ [OAUTH TOKEN] Processing OAuth 2.0 token request - clientId: {}, grantType: {}",
        clientId,
        grant_type);
    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();
    redirect_uri = URLUtils.decodeURLValue(redirect_uri);
    switch (GrantType.fromType(grant_type)) {
      case AUTHORIZATION_CODE, PASSWORD -> {
        LOG.debug(
            "📋 [OAUTH TOKEN] Delegating to getToken for grantType: {}, clientId: {}",
            grant_type,
            clientId);
        return getToken(
            clientId, grant_type, scope, code, prompt, redirect_uri, basic, request, response);
      }
      case REFRESH_TOKEN -> {
        LOG.debug("🔄 [OAUTH TOKEN] Delegating to refreshToken for clientId: {}", clientId);
        return refreshToken(clientId, refresh_token, grant_type, scope, request);
      }
      default -> {
        LOG.error(
            "❌ [OAUTH TOKEN] Invalid or unsupported grant_type '{}' for clientId: {}",
            grant_type,
            clientId);
        throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid o not supported grant_type!");
      }
    }
  }

  /**
   * Esegue il flusso RFC 8693 OAuth 2.0 Token Exchange per scambiare un token esistente con un
   * nuovo token destinato a un diverso client target.
   *
   * @param bearer Token di autorizzazione chiamante.
   * @param tokenExchange Dati della richiesta di Token Exchange.
   * @param exchange Contesto HTTP server.
   * @return Mono con la risposta contenente il nuovo token scambiato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> tokenExchange(
      String bearer, TokenExchange tokenExchange, ServerWebExchange exchange) {

    LOG.info(
        "🔄 [TOKEN EXCHANGE] Token Exchange process started for target client: {}",
        tokenExchange.getClient_id());

    // Recupera le credenziali del client target e del client sorgente
    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(tokenExchange.getClient_id());

    Mono<OAuth2Client> clientCredentialToExchangeMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(oAuth2Authorization.getClientId());

    return clientCredentialMono
        .zipWith(clientCredentialToExchangeMono)
        .flatMap(
            objects -> {
              LOG.debug(
                  "🔍 [TOKEN EXCHANGE] Validating token exchange parameters for client: {}",
                  tokenExchange.getClient_id());
              OAuthValidator.validateTokenExchange(bearer, tokenExchange, objects.getT1());
              return authService.exchangeToken(
                  tokenExchange,
                  oAuth2Authorization,
                  objects.getT1(),
                  objects.getT2(),
                  exchange.getRequest());
            })
        .doOnSuccess(
            responseResponseEntity ->
                LOG.info(
                    "✅ [TOKEN EXCHANGE] Token Exchange process completed successfully for client: {}",
                    tokenExchange.getClient_id()));
  }

  /**
   * Convalida il token Bearer fornito direttamente nell'endpoint authorize e ne verifica ruoli e
   * appartenenza client.
   *
   * @param bearer Token bearer ricevuto.
   * @param oAuth2Client Credenziali del client configurato.
   * @param exchange Contesto HTTP.
   * @return ResponseEntity con l'esito della validazione.
   */
  private Mono<ResponseEntity<?>> handleBearerToken(
      String bearer, OAuth2Client oAuth2Client, ServerWebExchange exchange, String state) {
    LOG.info("🛡️ [OAUTH BEARER] Bearer token detected, executing authorization validation");

    try {
      OAuth2Authorization oAuth2Authorization =
          oAuth2AuthorizationService.getByAccessTokenOrIdToken(bearer);
      OAuthValidator.validateUserRoles(oAuth2Client, oAuth2Authorization.getRoles());
      OAuthValidator.validateClientId(
          oAuth2Authorization.getClientId(), oAuth2Client.getClientId());

      AuthToken token = new AuthToken();
      token.setAccess_token(bearer.contains("Bearer") ? bearer.split("Bearer ")[1] : bearer);
      token.setToken_type("Bearer");
      token.setExpires_at(oAuth2Authorization.getAccessExpireDate());

      boolean includeUserData =
          (!ObjectUtils.isEmpty(exchange.getRequest().getQueryParams().get("include_user_data"))
                  && Boolean.parseBoolean(
                      exchange.getRequest().getQueryParams().get("include_user_data").getFirst()))
              || (!ObjectUtils.isEmpty(
                      exchange.getRequest().getQueryParams().get("includeUserData"))
                  && Boolean.parseBoolean(
                      exchange.getRequest().getQueryParams().get("includeUserData").getFirst()));

      if (includeUserData) {
        Mono<ResponseEntity<Response>> userResponse =
            userService
                .getUserByIdentifier(oAuth2Authorization.getIdentifier())
                .map(
                    user -> {
                      user.setPassword(null);
                      OAuthTokenResponse tokenResponse = new OAuthTokenResponse(token, user);
                      if (!ObjectUtils.isEmpty(state)) {
                        tokenResponse.setState(state);
                      }
                      Response response =
                          new Response(
                              HttpStatus.OK.value(),
                              "Token is valid",
                              TraceUtils.getSpanID(),
                              tokenResponse);
                      LOG.info(
                          "✅ [OAUTH BEARER] Authorization successful with user data for clientId: {}",
                          oAuth2Client.getClientId());
                      return ResponseEntity.ok(response);
                    })
                .defaultIfEmpty(
                    ResponseEntity.ok(
                        new Response(
                            HttpStatus.OK.value(),
                            "Token is valid",
                            TraceUtils.getSpanID(),
                            new OAuthTokenResponse(token))));
        return userResponse.map(r -> (ResponseEntity<?>) r);
      }

      OAuthTokenResponse tokenResponse = new OAuthTokenResponse(token);
      if (!ObjectUtils.isEmpty(state)) {
        tokenResponse.setState(state);
      }
      Response response =
          new Response(
              HttpStatus.OK.value(), "Token is valid", TraceUtils.getSpanID(), tokenResponse);

      LOG.info(
          "✅ [OAUTH BEARER] Authorization successful for clientId: {}", oAuth2Client.getClientId());
      return Mono.just(ResponseEntity.ok(response));

    } catch (TokenException | OAuthException e) {
      return Mono.just(handleInvalidToken(oAuth2Client, e, exchange));
    }
  }

  /**
   * Gestisce gli errori di validazione del token cancellando i cookie di sessione ed eseguendo il
   * redirect alla fallback URI (se configurata) o restituendo 401 Unauthorized.
   *
   * @param oAuth2Client Credenziali del client.
   * @param e Eccezione intercettata.
   * @param exchange Contesto HTTP.
   * @return ResponseEntity con fallback redirect o status non autorizzato.
   */
  private ResponseEntity<?> handleInvalidToken(
      OAuth2Client oAuth2Client, Exception e, ServerWebExchange exchange) {
    LOG.warn(
        "❌ [OAUTH ERROR] Token validation failed for clientId {}: {}",
        oAuth2Client.getClientId(),
        e.getMessage());

    /** Deleting all cookie if validation failed */
    LOG.debug("🧹 [OAUTH CLEANUP] Purging all cookies due to invalid token");
    Utils.deleteAllCookie(exchange.getResponse());

    if (e instanceof OAuthException exception
        && exception.getExceptionCode().equals(ExceptionMap.ERR_OAUTH_401)) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    if (!ObjectToolkit.isNullOrEmpty(oAuth2Client.getRedirect_uri())
        && !ObjectToolkit.isNullOrEmpty(
            oAuth2Client.getRedirect_uri().get(RedirectUris.FALLBACK_URI.url()))) {

      URI fallbackLocation =
          URI.create(oAuth2Client.getRedirect_uri().get(RedirectUris.FALLBACK_URI.url()));

      Response response =
          new Response(
              HttpStatus.FOUND.value(),
              "Token is not valid",
              TraceUtils.getSpanID(),
              fallbackLocation);

      LOG.info("↪️ [OAUTH FALLBACK] Redirecting to fallback URI: {}", fallbackLocation);
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

  /**
   * Costruisce il reindirizzamento verso l'identity provider configurato (Google OAuth o form di
   * login). Imposta i cookie di contesto per redirect URI e registration token.
   *
   * @param oAuth2Client Credenziali del client.
   * @param responseType Tipo di risposta richiesta.
   * @param accessType Tipo di accesso (online/offline).
   * @param finalRedirectUri URI di reindirizzamento finale decodificata.
   * @param scope Scope richiesti.
   * @param registrationToken Token di registrazione opzionale.
   * @param exchange Contesto HTTP.
   * @return Risposta di reindirizzamento HTTP verso la location autorizzativa.
   */
  private ResponseEntity<?> handleOAuthAuthorization(
      OAuth2Client oAuth2Client,
      String responseType,
      String accessType,
      String finalRedirectUri,
      String scope,
      String registrationToken,
      String state,
      String prompt,
      ServerWebExchange exchange) {

    ServerHttpRequest request = exchange.getRequest();
    ServerHttpResponse response = exchange.getResponse();

    OAuthValidator.validateClient(oAuth2Client, responseType, accessType, finalRedirectUri, scope);
    OAuthValidator.validatePrompt(prompt);
    URI location =
        GrpcService.getGoogleOAuthLocation(
            scope, finalRedirectUri, accessType, state, prompt, oAuth2Client);

    if (!ObjectToolkit.isNullOrEmpty(finalRedirectUri))
      CookieManager.setCookieInResponse(
          Cookie.COOKIE_REDIRECT_URI, finalRedirectUri, cookieDomain, response);
    if (!ObjectToolkit.isNullOrEmpty(registrationToken))
      CookieManager.setCookieInResponse(
          Cookie.COOKIE_TOKEN, registrationToken, cookieDomain, response);
    if (!ObjectToolkit.isNullOrEmpty(state))
      CookieManager.setCookieInResponse(Cookie.COOKIE_STATE, state, cookieDomain, response);

    LOG.info(
        "✅ [OAUTH AUTHORIZE] Authorization redirect prepared - clientId: {}, accessType: {}, responseType: {}, state: {}, prompt: {}, targetLocation: {}",
        oAuth2Client.getClientId(),
        accessType,
        responseType,
        state,
        prompt,
        location);

    return ResponseEntity.status(oAuth2Client.getAuthorize_redirect_status())
        .location(location)
        .build();
  }

  /**
   * Esegue l'emissione del token per grant type Bearer o Google OAuth. Coordina l'autenticazione
   * tramite {@link AuthService#makeClassicLogin} o {@link GoogleAuthService#performGoogleLogin}.
   */
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
        "🗝️ [OAUTH TOKEN] Starting oAuth/2.0/token processing - clientId: {}, grantType: {}, prompt: {}",
        clientId,
        grant_type,
        prompt);
    OAuthValidator.validateOAuthToken(clientId, grant_type);
    OAuthValidator.validatePrompt(prompt);
    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              OAuthType authType =
                  OAuthMapper.getOAuthType(clientCredential, oAuth2Authorization, grant_type, code);
              LOG.debug(
                  "🔍 [OAUTH TOKEN] Resolved OAuth authentication type: {} for clientId: {}",
                  authType,
                  clientId);
              switch (authType) {
                case BEARER -> {
                  LOG.info(
                      "👤 [OAUTH TOKEN] Performing classic Bearer login for clientId: {}",
                      clientId);
                  String redirectUri = redirect_uri;
                  if (ObjectUtils.isEmpty(redirectUri))
                    redirectUri =
                        RequestManager.getCookieOrHeaderData(Cookie.COOKIE_REDIRECT_URI, request);
                  OAuthValidator.validateBasicAuth(
                      basic, grant_type, redirectUri, clientCredential);
                  return authService.makeClassicLogin(
                      basic, redirectUri, clientCredential, request, response);
                }
                case GOOGLE -> {
                  LOG.info(
                      "🌐 [OAUTH TOKEN] Starting Google OAuth 2.0 authentication for clientId: {}",
                      clientId);
                  String redirectUri = redirect_uri;
                  if (ObjectUtils.isEmpty(redirectUri))
                    redirectUri =
                        RequestManager.getCookieOrHeaderData(Cookie.COOKIE_REDIRECT_URI, request);
                  OAuthValidator.validateOAuthGoogle(
                      clientCredential, code, scope, redirectUri, grant_type);
                  GoogleModel googleModel =
                      GrpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
                  return googleAuthService
                      .performGoogleLogin(
                          googleModel, clientCredential, redirectUri, request, response)
                      .doOnSuccess(
                          responseResponseEntity ->
                              LOG.info(
                                  "✅ [OAUTH TOKEN] Google OAuth 2.0 Login successfully completed for clientId: {}",
                                  clientId));
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
                  "✅ [OAUTH TOKEN] Ending oAuth/2.0/token flow successfully - clientId: {}, grantType: {}",
                  clientId,
                  grant_type);
            });
  }

  /**
   * Esegue il rinnovo del token per il client indicato a partire dal refresh token fornito.
   * Supporta sia token Bearer sia account federati Google.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> refreshToken(
      String clientId,
      String refresh_token,
      String grant_type,
      String scope,
      ServerHttpRequest request) {
    LOG.info(
        "🔄 [OAUTH REFRESH] Starting refresh_token endpoint - clientId: {}, grantType: {}",
        clientId,
        grant_type);
    OAuthValidator.validateRefreshToken(refresh_token, grant_type);
    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential -> {
              OAuth2Authorization refreshTokenData =
                  oAuth2AuthorizationService.getByRefreshToken(refresh_token);
              OAuthType authType =
                  OAuthMapper.getOAuthType(clientCredential, refreshTokenData, null, null);
              LOG.debug(
                  "🔍 [OAUTH REFRESH] Resolved refresh authType: {} for clientId: {}",
                  authType,
                  clientId);
              OAuthValidator.validateRefreshTokenData(refreshTokenData, clientCredential, authType);
              switch (authType) {
                case BEARER -> {
                  LOG.info("👤 [OAUTH REFRESH] Refreshing Bearer token for clientId: {}", clientId);
                  return authService.refreshToken(refreshTokenData, clientCredential, request);
                }
                case GOOGLE -> {
                  LOG.info(
                      "🌐 [OAUTH REFRESH] Refreshing Google OAuth token for clientId: {}",
                      clientId);
                  return googleAuthService.refreshGoogleToken(
                      refreshTokenData, clientCredential, request);
                }
                default -> {
                  return defaultErrorType();
                }
              }
            })
        .doOnSuccess(
            responseEntity ->
                LOG.info(
                    "✅ [OAUTH REFRESH] Ending refresh_token endpoint successfully - clientId: {}, grantType: {}",
                    clientId,
                    grant_type));
  }

  /**
   * Esegue il logout dell'utente revocando la sessione e invalidando i cookie di autenticazione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> logout(
      String clientId, String redirect_uri, String bearer, ServerHttpResponse response) {
    LOG.info("🚪 [OAUTH LOGOUT] Starting oAuth/2.0/logout endpoint for clientId: {}", clientId);
    Mono<OAuth2Client> clientCredentialMono =
        oAuth2ClientDomainService.getOAuth2ClientByClientID(clientId);

    if (isLogoutAlreadyDone()) {
      return createLogoutSuccessResponse(clientId, redirect_uri, response);
    }

    OAuth2Authorization tokenData = new OAuth2Authorization();
    BeanUtils.copyProperties(oAuth2Authorization, tokenData);
    OAuthValidator.validateClientId(tokenData.getClientId(), clientId);

    return clientCredentialMono
        .flatMap(
            clientCredential ->
                processLogoutByAuthType(tokenData, clientCredential, redirect_uri, response))
        .doOnSuccess(
            responseEntity -> {
              Utils.deleteAllCookie(response);
              LOG.info(
                  "✅ [OAUTH LOGOUT] Ending oAuth/2.0/logout endpoint successfully for clientId: {}",
                  clientId);
            });
  }

  private boolean isLogoutAlreadyDone() {
    return ObjectUtils.isEmpty(oAuth2Authorization.getClientId());
  }

  private Mono<ResponseEntity<?>> createLogoutSuccessResponse(
      String clientId, String redirect_uri, ServerHttpResponse response) {
    LOG.warn(
        "⚠️ [OAUTH LOGOUT] Session data is empty or logout already completed for clientId: {}",
        clientId);
    SessionID.invalidateSessionID(response);
    String message = "Logout Successfully!";
    Response res = new Response(HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);
    ResponseEntity<Response> responseResponseEntity = ResponseEntity.ok(res);
    if (!ObjectUtils.isEmpty(redirect_uri)) {
      responseResponseEntity =
          ResponseEntity.status(HttpStatus.OK).location(URI.create(redirect_uri)).body(res);
    }
    LOG.info("✅ [OAUTH LOGOUT] Returning logout success response for clientId: {}", clientId);
    return Mono.just(responseResponseEntity);
  }

  private Mono<ResponseEntity<?>> processLogoutByAuthType(
      OAuth2Authorization oAuth2Authorization,
      OAuth2Client oAuth2Client,
      String redirect_uri,
      ServerHttpResponse response) {
    OAuthType authType = OAuthMapper.getOAuthType(oAuth2Client, oAuth2Authorization, null, null);
    LOG.debug(
        "🔍 [OAUTH LOGOUT] Processing logout for authType: {}, subject: {}",
        authType,
        oAuth2Authorization.getSubject());
    return switch (authType) {
      case BEARER -> authService.logout(redirect_uri, oAuth2Authorization, response);
      case GOOGLE ->
          googleAuthService.processGoogleLogout(redirect_uri, oAuth2Authorization, response);
      default -> defaultErrorType();
    };
  }

  private Mono<ResponseEntity<?>> defaultErrorType() {
    LOG.error("❌ [OAUTH ERROR] Authentication type not configured or mismatched on client");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }
}
