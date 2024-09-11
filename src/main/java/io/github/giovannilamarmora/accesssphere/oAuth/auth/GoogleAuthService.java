package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.config.Cookie;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleGrpcMapper;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.utils.context.TraceUtils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
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

  @Autowired private TokenService tokenService;
  @Autowired private DataService dataService;
  @Autowired private AuthService authService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> performGoogleLogin(
      GoogleModel googleModel,
      ClientCredential clientCredential,
      ServerHttpRequest request,
      ServerHttpResponse serverHttpResponse) {
    boolean includeUserInfo =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_info"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_info").getFirst());
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    return dataService
        .getUserByEmail(googleModel.getJwtData().getEmail())
        .map(
            user -> {
              googleModel.getJwtData().setIdentifier(user.getIdentifier());
              googleModel.getJwtData().setRoles(user.getRoles());
              googleModel.getJwtData().setSub(user.getUsername());
              AuthToken token =
                  tokenService.generateToken(
                      googleModel.getJwtData(), clientCredential, googleModel.getTokenResponse());

              // TODO: Implementa mapper
              JsonNode strapi_token = null;
              try {
                String tokenValue = user.getAttributes().get("strapi-token").toString();
                if (!ObjectUtils.isEmpty(tokenValue)) {
                  // Correggi la stringa JSON aggiungendo la chiusura }
                  String jsonString = "{\"access_token\":\"" + tokenValue + "\"}";
                  strapi_token = MapperUtils.mapper().build().readTree(jsonString);
                }
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Login successfully, welcome " + user.getUsername() + " !",
                      TraceUtils.getSpanID(),
                      includeUserInfo
                          ? new OAuthTokenResponse(token, strapi_token, googleModel.getJwtData())
                          : (ObjectUtils.isEmpty(strapi_token)
                              ? token
                              : new OAuthTokenResponse(token, strapi_token)));
              CookieManager.setCookieInResponse(
                  Cookie.COOKIE_ACCESS_TOKEN.getCookie(),
                  token.getAccess_token(),
                  serverHttpResponse);
              return ResponseEntity.ok(response);
            })
        .onErrorResume(
            throwable -> {
              if (throwable
                  .getMessage()
                  .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                String registration_token =
                    CookieManager.getCookie(Cookie.COOKIE_TOKEN.getCookie(), request);
                if (ObjectUtils.isEmpty(registration_token)) {
                  LOG.error("Missing registration_token");
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_403,
                      "Missing registration_token, you cannot proceed!");
                }
                if (!registration_token.equalsIgnoreCase(clientCredential.getRegistrationToken())) {
                  LOG.error("Invalid registration_token");
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_403,
                      "Invalid registration_token, you cannot proceed!");
                }
                User userGoogle = GoogleGrpcMapper.generateGoogleUser(googleModel);
                userGoogle.setPassword(
                    CookieManager.getCookie(Cookie.COOKIE_TOKEN.getCookie(), request));
                return dataService
                    .registerUser(userGoogle, clientCredential)
                    .map(
                        user1 -> {
                          googleModel
                              .getJwtData()
                              .setRoles(
                                  ObjectUtils.isEmpty(clientCredential.getDefaultRole())
                                      ? null
                                      : List.of(clientCredential.getDefaultRole().getRole()));
                          AuthToken token =
                              tokenService.generateToken(
                                  googleModel.getJwtData(),
                                  clientCredential,
                                  googleModel.getTokenResponse());
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Login successfully, welcome " + user1.getUsername() + "!",
                                  TraceUtils.getSpanID(),
                                  includeUserInfo
                                      ? new OAuthTokenResponse(
                                          token,
                                          googleModel.getJwtData(),
                                          includeUserData ? user1 : null)
                                      : includeUserData
                                          ? new OAuthTokenResponse(token, user1)
                                          : token);
                          return ResponseEntity.ok(response);
                        });
              }
              return Mono.error(throwable);
            });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> refreshGoogleToken(
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

    GoogleModel googleModel = GrpcService.refreshToken(token, clientCredential);

    return dataService
        .getUserByEmail(googleModel.getJwtData().getEmail())
        .map(
            user -> {
              googleModel.getJwtData().setIdentifier(user.getIdentifier());
              googleModel.getJwtData().setRoles(user.getRoles());
              googleModel.getJwtData().setSub(user.getUsername());
              AuthToken authToken =
                  tokenService.generateToken(
                      googleModel.getJwtData(), clientCredential, googleModel.getTokenResponse());
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
      String redirect_uri, AccessTokenData accessTokenData, ServerHttpResponse response) {
    LOG.info("Google oAuth 2.0 Logout started");
    String accessToken = accessTokenData.getPayload().get("access_token").textValue();
    GrpcService.logout(accessToken);
    return authService
        .logout(redirect_uri, accessTokenData, response)
        .doOnSuccess(responseResponseEntity -> LOG.info("Google oAuth 2.0 Logout ended"));
  }
}
