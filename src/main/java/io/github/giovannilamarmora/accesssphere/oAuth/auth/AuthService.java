package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.data.tech.TechUserService;
import io.github.giovannilamarmora.accesssphere.data.user.UserService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.SubjectType;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.token.model.TokenExchange;
import io.github.giovannilamarmora.accesssphere.utilities.Cookie;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.net.URI;
import java.util.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

/**
 * Servizio di autenticazione e gestione token per il protocollo OAuth 2.0.
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Decodifica le credenziali HTTP Basic e le valida delegando la verifica BCrypt a {@link
 *       UserService#authenticateUser}.
 *   <li>Genera i claim JWT standard e compila i token di accesso/refresh tramite {@link
 *       TokenService#generateToken}.
 *   <li>Gestisce il rinnovo dei token di sessione (refresh grant) e la revoca asincrona tramite
 *       {@link AccessTokenService#revokeTokenByIdentifier}.
 *   <li>Imposta i cookie di sessione sicuri nella risposta HTTP e determina i redirect URI
 *       configurati sul client OAuth2.
 * </ul>
 */
@Service
@Logged
public class AuthService {

  private static final Logger LOG = LoggerFilter.getLogger(AuthService.class);

  @Value("${cookie-domain:}")
  private String cookieDomain;

  @Autowired private TokenService tokenService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired @Lazy private UserService userService;
  @Autowired private TechUserService techUserService;

  /**
   * Esegue l'autenticazione tramite credenziali classiche (username o email e password).
   *
   * @param basic Stringa Base64 contenente le credenziali nell'header HTTP Authorization Basic.
   * @param redirect_uri URI di redirect opzionale configurato sul client.
   * @param clientCredential Credenziali e configurazione del client OAuth2 chiamante.
   * @param request Richiesta HTTP reattiva corrente.
   * @param serverHttpResponse Risposta HTTP reattiva corrente per il posizionamento dei cookie.
   * @return Mono contenente ResponseEntity con stato 200 OK e il payload dei token generati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> makeClassicLogin(
      String basic,
      String redirect_uri,
      ClientCredential clientCredential,
      ServerHttpRequest request,
      ServerHttpResponse serverHttpResponse) {
    LOG.debug("🔑 Decoding user using Base64 Decoder");
    String username;
    String password;
    try {
      String[] decoded =
          new String(Base64.getDecoder().decode(basic.split("Basic ")[1])).split(":");
      username = decoded[0];
      password = decoded[1];
    } catch (Exception e) {
      LOG.error("❌ Error during decoding username and password, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, "Invalid basic provided!");
    }
    LOG.debug("👤 Login process started for user {}", username);

    // Controllo se ha usato l'email invece dello username
    String email = username.contains("@") ? username : null;
    username = email != null ? null : username;

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    return performLogin(username, email, password, clientCredential, request)
        .map(
            tokenResponse -> {
              OAuthValidator.validateUserRoles(
                  clientCredential, tokenResponse.getUser().getRoles());

              if (!ObjectToolkit.isNullOrEmpty(tokenResponse.getToken().getTemp_token())) {
                return ResponseEntity.status(HttpStatus.OK)
                    .body(
                        new Response(
                            HttpStatus.OK.value(),
                            "MFA Verification Required",
                            TraceUtils.getSpanID(),
                            new OAuthTokenResponse(tokenResponse.getToken())));
              }
              return setLoginResponse(
                  tokenResponse,
                  clientCredential,
                  includeUserInfo,
                  includeUserData,
                  redirect_uri,
                  cookieDomain,
                  serverHttpResponse);
            });
  }

  /**
   * Esegue la validazione delle credenziali utente e la generazione della risposta token completa.
   *
   * @param username Username dell'utente.
   * @param email Indirizzo email dell'utente.
   * @param password Password in chiaro da verificare.
   * @param clientCredential Credenziali del client OAuth.
   * @param request Richiesta HTTP server.
   * @return Mono contenente l'oggetto {@link OAuthTokenResponse}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> performLogin(
      String username,
      String email,
      String password,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    LOG.debug("🔑 Validating user credentials for username={} and email={}", username, email);
    return userService
        .authenticateUser(username, email, password)
        .map(
            authenticatedUser -> {
              JWTData jwtData =
                  JWTData.generateJWTData(
                      authenticatedUser, clientCredential, SubjectType.CUSTOMER, request);
              // AuthToken token =
              //    tokenService.generateToken(
              //        jwtData, clientCredential, authenticatedUser.getAttributes());
              AuthToken token = tokenService.generateToken(jwtData, clientCredential, null);
              LOG.debug(
                  "✅ Login process ended successfully for user {}",
                  authenticatedUser.getUsername());
              return new OAuthTokenResponse(authenticatedUser, jwtData, token);
            });
  }

  /**
   * Costruisce la risposta di login popolando cookie ed eventuale reindirizzamento.
   *
   * @param tokenResponse Risposta con token e anagrafica utente.
   * @param clientCredential Credenziali del client OAuth2.
   * @param includeUserInfo Flag per includere i claim OIDC.
   * @param includeUserData Flag per includere il DTO anagrafico utente.
   * @param redirect_uri URI di reindirizzamento opzionale.
   * @param cookieDomain Dominio per il salvataggio dei cookie di sessione.
   * @param httpResponse Risposta HTTP server.
   * @return ResponseEntity con stato 200 OK e Response popolata.
   */
  public static ResponseEntity<Response> setLoginResponse(
      OAuthTokenResponse tokenResponse,
      ClientCredential clientCredential,
      boolean includeUserInfo,
      boolean includeUserData,
      String redirect_uri,
      String cookieDomain,
      ServerHttpResponse httpResponse) {
    String message =
        "Login Successfully! Welcome back " + tokenResponse.getUser().getUsername() + "!";

    OAuthTokenResponse tokenData =
        new OAuthTokenResponse(
            tokenResponse.getToken(),
            includeUserInfo ? tokenResponse.getUserInfo() : null,
            includeUserData ? tokenResponse.getUser() : null);
    if (!ObjectToolkit.isNullOrEmpty(tokenResponse.getUser())
        && !ObjectToolkit.isNullOrEmpty(tokenResponse.getUser().getAttributes())) {
      try {
        tokenData.setPayload(
            io.github.giovannilamarmora.utils.utilities.Mapper.readObject(
                io.github.giovannilamarmora.utils.utilities.Mapper.writeObjectToString(
                    tokenResponse.getUser().getAttributes()),
                com.fasterxml.jackson.databind.JsonNode.class));
      } catch (Exception e) {
        LOG.warn("⚠️ Failed to serialize user attributes to payload: {}", e.getMessage());
      }
    }

    Response response =
        new Response(HttpStatus.OK.value(), message, TraceUtils.getSpanID(), tokenData);

    LOG.info("✅ Login process ended for user {}", tokenResponse.getUser().getUsername());
    if (ObjectUtils.isEmpty(redirect_uri)) return ResponseEntity.ok(response);

    CookieManager.setCookieInResponse(
        Cookie.COOKIE_ACCESS_TOKEN,
        tokenResponse.getToken().getAccess_token(),
        cookieDomain,
        httpResponse);

    URI finalRedirectURI =
        OAuthMapper.getFinalRedirectURI(
            clientCredential, RedirectUris.POST_LOGIN_URL, redirect_uri);
    return ResponseEntity.ok().location(finalRedirectURI).body(response);
  }

  /**
   * Rinnova il token JWT di sessione a partire dai dati dell'access token corrente.
   *
   * @param accessTokenData Dati decodificati della sessione utente.
   * @param clientCredential Credenziali del client OAuth.
   * @param request Richiesta HTTP server.
   * @return Mono contenente ResponseEntity con esito del rinnovo.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> refreshToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      ServerHttpRequest request) {

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    return refreshJWTToken(accessTokenData, clientCredential, request)
        .map(
            tokenResponse -> {
              String message =
                  "Refresh Token Successfully! Welcome back "
                      + tokenResponse.getUser().getUsername()
                      + "!";

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      message,
                      TraceUtils.getSpanID(),
                      new OAuthTokenResponse(
                          tokenResponse.getToken(),
                          includeUserInfo ? tokenResponse.getUserInfo() : null,
                          includeUserData ? tokenResponse.getUser() : null));
              return ResponseEntity.ok(response);
            });
  }

  /**
   * Esegue il rinnovo del token JWT estraendo i dati utente e rigenerando la firma del token.
   *
   * @param accessTokenData Dati del token di accesso da rinnovare.
   * @param clientCredential Credenziali del client OAuth.
   * @param request Richiesta HTTP server.
   * @return Mono contenente {@link OAuthTokenResponse} con il nuovo token generato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<OAuthTokenResponse> refreshJWTToken(
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      ServerHttpRequest request) {
    LOG.info("🔄 Refreshing JWT token for user email: {}", accessTokenData.getEmail());
    return userService
        .getUserByEmail(accessTokenData.getEmail())
        .map(
            user -> {
              user.setPassword(null);
              JWTData jwtData =
                  JWTData.generateJWTData(
                      user, clientCredential, accessTokenData.getSubjectType(), request);
              AuthToken authToken = tokenService.generateToken(jwtData, clientCredential, null);
              return new OAuthTokenResponse(user, jwtData, authToken);
            });
  }

  /**
   * Esegue il logout dell'utente revocando i token associati nel registro token.
   *
   * @param redirect_uri URI di ritorno post-logout.
   * @param accessTokenData Dati del token di accesso dell'utente.
   * @param response Risposta HTTP server.
   * @return Mono contenente ResponseEntity di conferma logout.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> logout(
      String redirect_uri, AccessTokenData accessTokenData, ServerHttpResponse response) {
    String refreshToken =
        (!ObjectToolkit.isNullOrEmpty(accessTokenData.getPayload())
                && !ObjectToolkit.isNullOrEmpty(accessTokenData.getPayload().get("refresh_token")))
            ? accessTokenData.getPayload().get("refresh_token").textValue()
            : null;

    if (ObjectUtils.isEmpty(refreshToken)) {
      LOG.info("⚠️ Refresh token not found in payload");
    }

    String message = "Logout Successfully for " + accessTokenData.getSubject() + "!";
    Response res = new Response(HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);

    ResponseEntity<Response> responseResponseEntity = ResponseEntity.ok(res);
    if (!ObjectUtils.isEmpty(redirect_uri))
      responseResponseEntity =
          ResponseEntity.status(HttpStatus.OK).location(URI.create(redirect_uri)).body(res);

    LOG.info("🚪 Logging out user: {}", accessTokenData.getIdentifier());
    accessTokenService.revokeTokenByIdentifier(accessTokenData.getIdentifier());
    SessionID.invalidateSessionID(response);

    return Mono.just(responseResponseEntity);
  }

  /**
   * Esegue lo scambio di token (Token Exchange RFC 8693) tra client OAuth2.
   *
   * @param tokenExchange Richiesta di token exchange.
   * @param accessTokenData Dati del token di accesso del chiamante.
   * @param clientCredential Credenziali del client ricevente.
   * @param clientCredentialToExchange Credenziali del client emittente del token soggetto.
   * @param request Richiesta HTTP server.
   * @return Mono contenente la risposta {@link ResponseEntity} con il token scambiato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> exchangeToken(
      TokenExchange tokenExchange,
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      ClientCredential clientCredentialToExchange,
      ServerHttpRequest request) {
    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    JWTData decryptToken =
        tokenService.parseToken(tokenExchange.getSubject_token(), clientCredentialToExchange);

    if (techUserService.isTechUser()) {
      return Mono.just(
          exchangeTokenBuildResponse(
              tokenExchange,
              new User(),
              accessTokenData,
              clientCredential,
              includeUserInfo,
              includeUserData,
              request));
    }
    return userService
        .getUserByEmail(decryptToken.getEmail())
        .map(
            user ->
                exchangeTokenBuildResponse(
                    tokenExchange,
                    user,
                    accessTokenData,
                    clientCredential,
                    includeUserInfo,
                    includeUserData,
                    request));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private ResponseEntity<Response> exchangeTokenBuildResponse(
      TokenExchange tokenExchange,
      User user,
      AccessTokenData accessTokenData,
      ClientCredential clientCredential,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
    AuthToken token = tokenService.exchangeToken(user, accessTokenData, clientCredential, request);
    String message =
        "Token for client " + tokenExchange.getClient_id() + " successfully exchanged!";
    Response response =
        new Response(
            HttpStatus.OK.value(),
            message,
            TraceUtils.getSpanID(),
            new OAuthTokenResponse(
                token,
                accessTokenData.getPayload(),
                includeUserInfo
                    ? tokenService.parseToken(token.getAccess_token(), clientCredential)
                    : null,
                includeUserData ? user : null));
    return ResponseEntity.ok(response);
  }
}
