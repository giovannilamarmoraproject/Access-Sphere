package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.config.AppConfig;
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
import io.github.giovannilamarmora.accesssphere.utilities.LoggerFilter;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class GoogleAuthService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());

  @Autowired private TokenService tokenService;
  @Autowired private DataService dataService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> performGoogleLogin(
      GoogleModel googleModel, ClientCredential clientCredential, ServerHttpRequest request) {
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
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Login successfully, welcome " + user.getUsername() + " !",
                      CorrelationIdUtils.getCorrelationId(),
                      includeUserInfo
                          ? new OAuthTokenResponse(token, googleModel.getJwtData())
                          : token);
              return ResponseEntity.ok(response);
            })
        .onErrorResume(
            throwable -> {
              if (throwable
                  .getMessage()
                  .equalsIgnoreCase(ExceptionMap.ERR_STRAPI_404.getMessage())) {
                String registration_token = Utils.getCookie(AppConfig.COOKIE_TOKEN, request);
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
                userGoogle.setPassword(Utils.getCookie(AppConfig.COOKIE_TOKEN, request));
                return dataService
                    .registerUser(userGoogle, clientCredential)
                    .map(
                        user1 -> {
                          googleModel
                              .getJwtData()
                              .setRoles(
                                  ObjectUtils.isEmpty(clientCredential.getDefaultRoles())
                                      ? null
                                      : clientCredential.getDefaultRoles().stream()
                                          .map(AppRole::getRole)
                                          .toList());
                          AuthToken token =
                              tokenService.generateToken(
                                  googleModel.getJwtData(),
                                  clientCredential,
                                  googleModel.getTokenResponse());
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Login successfully, welcome " + user1.getUsername() + "!",
                                  CorrelationIdUtils.getCorrelationId(),
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
                      CorrelationIdUtils.getCorrelationId(),
                      includeUserInfo
                          ? new OAuthTokenResponse(
                              authToken, googleModel.getJwtData(), includeUserData ? user : null)
                          : includeUserData ? new OAuthTokenResponse(authToken, user) : authToken);
              return ResponseEntity.ok(response);
            });
  }
}
