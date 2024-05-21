package io.github.giovannilamarmora.accesssphere.oAuth.auth;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleModel;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleOAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthService;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class AuthService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Autowired private TokenService tokenService;
  @Autowired private ClientService clientService;
  @Autowired private DataService dataService;
  @Autowired private GrpcService grpcService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> performGoogleLogin(
      GoogleModel googleModel,
      ClientCredential clientCredential,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
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
                String registration_token = Utils.getCookie(OAuthService.COOKIE_TOKEN, request);
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
                User userGoogle = GoogleOAuthMapper.generateGoogleUser(googleModel);
                userGoogle.setPassword(Utils.getCookie(OAuthService.COOKIE_TOKEN, request));
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
  public Mono<ResponseEntity<Response>> makeClassicLogin(
      String basic,
      ClientCredential clientCredential,
      boolean includeUserInfo,
      boolean includeUserData,
      ServerHttpRequest request) {
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

    return dataService
        .login(username, email, password, clientCredential, request)
        .map(
            tokenResponse -> {
              // TODO: [OPZIONALE] Non torno i ruoli perchè strapi non gestisce i ruoli alla login,
              // se li vuoi implementa la user/me
              String message =
                  "Login Successfully! Welcome back " + tokenResponse.getUser().getUsername() + "!";

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      message,
                      CorrelationIdUtils.getCorrelationId(),
                      new OAuthTokenResponse(
                          tokenResponse.getToken(),
                          tokenResponse.getStrapiToken(),
                          includeUserInfo ? tokenResponse.getUserInfo() : null,
                          includeUserData ? tokenResponse.getUser() : null));
              LOG.info("Login process ended for user {}", tokenResponse.getUser().getUsername());
              return ResponseEntity.ok(response);
            });
  }
}
