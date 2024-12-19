package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.accesssphere.utilities.Cookie;
import io.github.giovannilamarmora.accesssphere.utilities.SessionID;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.net.URI;
import java.util.Base64;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value("${cookie-domain}")
  private String cookieDomain;

  @Autowired private TokenService tokenService;
  @Autowired private DataService dataService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> makeClassicLogin(
      String basic,
      String redirect_uri,
      ClientCredential clientCredential,
      ServerHttpRequest request,
      ServerHttpResponse serverHttpResponse) {
    LOG.debug("Decoding user using Base64 Decoder");
    String username;
    String password;
    try {
      String[] decoded =
          new String(Base64.getDecoder().decode(basic.split("Basic ")[1])).split(":");
      username = decoded[0];
      password = decoded[1];
    } catch (Exception e) {
      LOG.error("Error during decoding username and password, message is {}", e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, "Invalid basic provided!");
    }
    LOG.debug("Login process started for user {}", username);

    // Controllo se ha usato l'email invece dello username
    String email = username.contains("@") ? username : null;
    username = email != null ? null : username;

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());

    return dataService
        .login(username, email, password, clientCredential, request)
        .map(
            tokenResponse -> {
              OAuthValidator.validateUserRoles(
                  clientCredential, tokenResponse.getUser().getRoles());
              String message =
                  "Login Successfully! Welcome back " + tokenResponse.getUser().getUsername() + "!";

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      message,
                      TraceUtils.getSpanID(),
                      new OAuthTokenResponse(
                          tokenResponse.getToken(),
                          clientCredential.getStrapiToken() ? tokenResponse.getStrapiToken() : null,
                          includeUserInfo ? tokenResponse.getUserInfo() : null,
                          includeUserData ? tokenResponse.getUser() : null));

              LOG.info("Login process ended for user {}", tokenResponse.getUser().getUsername());
              if (ObjectUtils.isEmpty(redirect_uri)) return ResponseEntity.ok(response);
              CookieManager.setCookieInResponse(
                  Cookie.COOKIE_ACCESS_TOKEN,
                  tokenResponse.getToken().getAccess_token(),
                  cookieDomain,
                  serverHttpResponse);
              CookieManager.setCookieInResponse(
                  Cookie.COOKIE_STRAPI_TOKEN,
                  tokenResponse
                      .getStrapiToken()
                      .get(TokenData.STRAPI_ACCESS_TOKEN.getToken())
                      .asText(),
                  cookieDomain,
                  serverHttpResponse);
              return ResponseEntity.ok().location(URI.create(redirect_uri)).body(response);
            });
  }

  static void setCookieInResponseLocal(
      String cookieName, String cookieValue, ServerHttpResponse response) {
    ResponseCookie cookie =
        ResponseCookie.from(cookieName, cookieValue)
            .maxAge(360000L)
            .sameSite("None")
            .secure(true)
            .httpOnly(false)
            .path("/")
            .build();
    response.getHeaders().add("Set-Cookie", cookie.toString());
  }

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

    String token = accessTokenData.getPayload().get("refresh_token").textValue();

    return dataService
        .refreshJWTToken(accessTokenData, clientCredential, token, request)
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
                          tokenResponse.getStrapiToken(),
                          includeUserInfo ? tokenResponse.getUserInfo() : null,
                          includeUserData ? tokenResponse.getUser() : null));
              return ResponseEntity.ok(response);
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> logout(
      String redirect_uri, AccessTokenData accessTokenData, ServerHttpResponse response) {
    String refreshToken = accessTokenData.getPayload().get("refresh_token").textValue();

    if (ObjectUtils.isEmpty(refreshToken)) {
      LOG.info("Strapi refresh_token not found!");
    }

    String message = "Logout Successfully for " + accessTokenData.getEmail() + "!";
    Response res = new Response(HttpStatus.OK.value(), message, TraceUtils.getSpanID(), null);

    ResponseEntity<Response> responseResponseEntity = ResponseEntity.ok(res);
    if (!ObjectUtils.isEmpty(redirect_uri))
      responseResponseEntity =
          ResponseEntity.status(HttpStatus.OK).location(URI.create(redirect_uri)).body(res);

    return dataService
        .logout(refreshToken, accessTokenData)
        .doOnSuccess(
            unused -> {
              SessionID.invalidateSessionID(response);
            })
        .then(Mono.just(responseResponseEntity));
  }
}
