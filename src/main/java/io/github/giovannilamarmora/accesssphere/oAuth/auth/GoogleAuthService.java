package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcMapper;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcMapper;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthValidator;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.model.AuthToken;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.admin.UserAdminService;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.Cookie;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.CookieManager;
import io.github.giovannilamarmora.utils.web.RequestManager;
import java.net.URI;
import java.util.List;
import java.util.UUID;
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

@Service
public class GoogleAuthService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Value("${cookie-domain:}")
  private String cookieDomain;

  @Autowired private TokenService tokenService;
  @Autowired @Lazy private UserService userService;
  @Autowired @Lazy private UserAdminService userAdminService;
  @Autowired private AuthService authService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> performGoogleLogin(
      GoogleModel googleModel,
      OAuth2Client oAuth2Client,
      String redirect_uri,
      ServerHttpRequest request,
      ServerHttpResponse serverHttpResponse) {
    boolean includeUserInfo =
        (!ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("include_user_info").getFirst()))
            || (!ObjectUtils.isEmpty(request.getQueryParams().get("includeUserInfo"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("includeUserInfo").getFirst()));
    boolean includeUserData =
        (!ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("include_user_data").getFirst()))
            || (!ObjectUtils.isEmpty(request.getQueryParams().get("includeUserData"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("includeUserData").getFirst()));

    String state = request.getQueryParams().getFirst("state");
    if (ObjectUtils.isEmpty(state)) {
      state = RequestManager.getCookieOrHeaderData(Cookie.COOKIE_STATE, request);
    }
    final String finalState = state;

    return userService
        .getUserByEmail(googleModel.getJwtData().getEmail())
        .map(
            user -> {
              OAuthValidator.validateUserRoles(oAuth2Client, user.getRoles());
              googleModel.setJwtData(
                  GrpcMapper.setIdentifier(googleModel.getJwtData(), user.getIdentifier()));
              googleModel
                  .getJwtData()
                  .setRoles(
                      user.getRoles() != null
                          ? user.getRoles().stream()
                              .map(UserClientRole::getRole)
                              .filter(java.util.Objects::nonNull)
                              .toList()
                          : null);
              googleModel.getJwtData().setSub(user.getUsername());

              AuthToken token =
                  tokenService.generateToken(
                      googleModel.getJwtData(), oAuth2Client, googleModel.getTokenResponse());

              OAuthTokenResponse tokenData =
                  new OAuthTokenResponse(
                      token,
                      includeUserInfo ? googleModel.getJwtData() : null,
                      includeUserData ? user : null);
              if (!ObjectUtils.isEmpty(finalState)) {
                tokenData.setState(finalState);
              }

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Login successfully, welcome " + user.getUsername() + " !",
                      TraceUtils.getSpanID(),
                      tokenData);

              CookieManager.setCookieInResponse(
                  Cookie.COOKIE_ACCESS_TOKEN,
                  token.getAccess_token(),
                  cookieDomain,
                  serverHttpResponse);

              URI finalRedirectURI =
                  OAuthMapper.getFinalRedirectURI(
                      oAuth2Client, RedirectUris.POST_LOGIN_URL, redirect_uri);
              if (!ObjectUtils.isEmpty(finalState)
                  && !finalRedirectURI.toString().contains("state=")) {
                String separator = finalRedirectURI.toString().contains("?") ? "&" : "?";
                finalRedirectURI =
                    URI.create(
                        finalRedirectURI.toString()
                            + separator
                            + "state="
                            + java.net.URLEncoder.encode(
                                finalState, java.nio.charset.StandardCharsets.UTF_8));
              }
              return ResponseEntity.ok().location(finalRedirectURI).body(response);
            })
        .onErrorResume(
            throwable -> {
              if (throwable
                  .getMessage()
                  .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_404.getMessage())) {
                if (Boolean.FALSE.equals(oAuth2Client.getEnableRegistration())) {
                  LOG.error(
                      "❌ Google registration rejected: registration is disabled for client {}",
                      oAuth2Client.getClientId());
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_403,
                      "User registration is disabled for client " + oAuth2Client.getClientId());
                }
                User userGoogle = GoogleGrpcMapper.generateGoogleUser(googleModel);
                userGoogle.setPassword(UUID.randomUUID().toString());
                return userAdminService
                    .registerUser(null, userGoogle, oAuth2Client, true)
                    .map(
                        user1 -> {
                          RoleDefinition defaultRole =
                              (oAuth2Client.getAppRoles() == null)
                                  ? null
                                  : oAuth2Client.getAppRoles().stream()
                                      .filter(appRole -> appRole != null && appRole.isUser())
                                      .findFirst()
                                      .orElseGet(
                                          () ->
                                              oAuth2Client.getAppRoles().isEmpty()
                                                  ? null
                                                  : oAuth2Client.getAppRoles().getFirst());
                          googleModel.setJwtData(
                              GrpcMapper.setIdentifier(
                                  googleModel.getJwtData(), user1.getIdentifier()));
                          googleModel
                              .getJwtData()
                              .setRoles(
                                  ObjectUtils.isEmpty(defaultRole)
                                      ? null
                                      : List.of(defaultRole.getRole()));
                          AuthToken token =
                              tokenService.generateToken(
                                  googleModel.getJwtData(),
                                  oAuth2Client,
                                  googleModel.getTokenResponse());
                          OAuthTokenResponse tokenData =
                              new OAuthTokenResponse(
                                  token,
                                  includeUserInfo ? googleModel.getJwtData() : null,
                                  includeUserData ? user1 : null);
                          if (!ObjectUtils.isEmpty(finalState)) {
                            tokenData.setState(finalState);
                          }
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Login successfully, welcome " + user1.getUsername() + "!",
                                  TraceUtils.getSpanID(),
                                  tokenData);
                          URI finalRedirectURI =
                              OAuthMapper.getFinalRedirectURI(
                                  oAuth2Client, RedirectUris.POST_LOGIN_URL, redirect_uri);
                          if (!ObjectUtils.isEmpty(finalState)
                              && !finalRedirectURI.toString().contains("state=")) {
                            String separator =
                                finalRedirectURI.toString().contains("?") ? "&" : "?";
                            finalRedirectURI =
                                URI.create(
                                    finalRedirectURI.toString()
                                        + separator
                                        + "state="
                                        + java.net.URLEncoder.encode(
                                            finalState, java.nio.charset.StandardCharsets.UTF_8));
                          }
                          return ResponseEntity.ok().location(finalRedirectURI).body(response);
                        });
              }
              return Mono.error(throwable);
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> refreshGoogleToken(
      OAuth2Authorization oAuth2Authorization,
      OAuth2Client oAuth2Client,
      ServerHttpRequest request) {

    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        (!ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("include_user_data").getFirst()))
            || (!ObjectUtils.isEmpty(request.getQueryParams().get("includeUserData"))
                && Boolean.parseBoolean(
                    request.getQueryParams().get("includeUserData").getFirst()));

    String token = oAuth2Authorization.getPayload().get("refresh_token").textValue();

    GoogleModel googleModel = GrpcService.refreshToken(token, oAuth2Client);

    return userService
        .getUserByEmail(googleModel.getJwtData().getEmail())
        .map(
            user -> {
              googleModel.getJwtData().setIdentifier(user.getIdentifier());
              googleModel
                  .getJwtData()
                  .setRoles(
                      user.getRoles() != null
                          ? user.getRoles().stream()
                              .map(UserClientRole::getRole)
                              .filter(java.util.Objects::nonNull)
                              .toList()
                          : null);
              googleModel.getJwtData().setSub(user.getUsername());
              AuthToken authToken =
                  tokenService.generateToken(
                      googleModel.getJwtData(), oAuth2Client, googleModel.getTokenResponse());
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Token refreshed for " + user.getUsername() + "!",
                      TraceUtils.getSpanID(),
                      includeUserInfo
                          ? new OAuthTokenResponse(
                              authToken, googleModel.getJwtData(), includeUserData ? user : null)
                          : includeUserData ? new OAuthTokenResponse(authToken, user) : authToken);
              return ResponseEntity.ok(response);
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> processGoogleLogout(
      String redirect_uri, OAuth2Authorization oAuth2Authorization, ServerHttpResponse response) {
    LOG.info("Google oAuth 2.0 Logout started");
    String accessToken = oAuth2Authorization.getPayload().get("access_token").textValue();
    GrpcService.logout(accessToken);
    return authService
        .logout(redirect_uri, oAuth2Authorization, response)
        .doOnSuccess(responseResponseEntity -> LOG.info("Google oAuth 2.0 Logout ended"));
  }
}
