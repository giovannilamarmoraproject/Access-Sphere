package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.data.AccessTokenService;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class UserService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private DataService dataService;
  @Autowired private ClientService clientService;
  @Autowired private AccessTokenService accessTokenService;
  @Autowired private TokenService tokenService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> userInfo(String bearer, ServerHttpRequest request) {
    boolean includeUserData =
        !ObjectUtils.isEmpty(request.getQueryParams().get("include_user_data"))
            && Boolean.parseBoolean(request.getQueryParams().get("include_user_data").getFirst());
    LOG.info("UserInfo process started, include Data: {}", includeUserData);
    AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono.flatMap(
        clientCredential -> {
          UserValidator.validateAuthType(clientCredential, accessTokenData);
          JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
          switch (decryptToken.getType()) {
            case BEARER -> {
              return dataService
                  .getUserInfo(
                      decryptToken, accessTokenData.getPayload().get("access_token").textValue())
                  .map(
                      user -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "UserInfo Data for " + user.getUsername(),
                                CorrelationIdUtils.getCorrelationId(),
                                includeUserData
                                    ? new OAuthTokenResponse(decryptToken, user)
                                    : decryptToken);
                        return ResponseEntity.ok(response);
                      });
            }
            case GOOGLE -> {
              GoogleModel userInfo =
                  GrpcService.userInfo(
                      accessTokenData.getPayload().get("id_token").textValue(), clientCredential);
              UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
              if (includeUserData) {
                return dataService
                    .getUserByEmail(decryptToken.getEmail())
                    .map(
                        user -> {
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "UserInfo Data for " + user.getUsername(),
                                  CorrelationIdUtils.getCorrelationId(),
                                  new OAuthTokenResponse(decryptToken, user));
                          return ResponseEntity.ok(response);
                        });
              }
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "UserInfo Data for " + decryptToken.getSub(),
                      CorrelationIdUtils.getCorrelationId(),
                      decryptToken);
              return Mono.just(ResponseEntity.ok(response));
            }
            default -> {
              return UserValidator.defaultErrorOnType();
            }
          }
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> profile(String bearer, ServerHttpRequest request) {
    LOG.info("Getting profile process started");
    AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);

    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(accessTokenData.getClientId());

    return clientCredentialMono.flatMap(
        clientCredential -> {
          UserValidator.validateAuthType(clientCredential, accessTokenData);
          JWTData decryptToken = tokenService.parseToken(bearer, clientCredential);
          switch (decryptToken.getType()) {
            case BEARER -> {
              return dataService
                  .getUserInfo(
                      decryptToken, accessTokenData.getPayload().get("access_token").textValue())
                  .map(
                      user -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Profile Data for " + user.getUsername(),
                                CorrelationIdUtils.getCorrelationId(),
                                user);
                        return ResponseEntity.ok(response);
                      });
            }
            case GOOGLE -> {
              GoogleModel userInfo =
                  GrpcService.userInfo(
                      accessTokenData.getPayload().get("id_token").textValue(), clientCredential);
              UserValidator.validateGoogleUserInfo(userInfo, decryptToken);
              return dataService
                  .getUserByEmail(decryptToken.getEmail())
                  .map(
                      user -> {
                        Response response =
                            new Response(
                                HttpStatus.OK.value(),
                                "Profile Data for " + user.getUsername(),
                                CorrelationIdUtils.getCorrelationId(),
                                user);
                        return ResponseEntity.ok(response);
                      });
            }
            default -> {
              return UserValidator.defaultErrorOnType();
            }
          }
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> register(
      User user, String clientId, String registration_token) throws UtilsException {
    LOG.info(
        "Registration process started, username: {}, email: {}",
        user.getUsername(),
        user.getEmail());
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          UserValidator.validateRegistration(registration_token, clientCredential, user);
          return dataService
              .registerUser(user, clientCredential)
              .map(
                  user1 -> {
                    Response response =
                        new Response(
                            HttpStatus.OK.value(),
                            "User " + user.getUsername() + " successfully registered!",
                            CorrelationIdUtils.getCorrelationId(),
                            user1);
                    return ResponseEntity.status(HttpStatus.CREATED).body(response);
                  });
        });
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<Response>> updateUser(
      User userToUpdate, String bearer, ServerHttpRequest request) throws UtilsException {
    userToUpdate.setPassword(null);
    LOG.info(
        "Updating user process started, username: {}, email: {}",
        userToUpdate.getUsername(),
        userToUpdate.getEmail());

    AccessTokenData accessTokenData = accessTokenService.getByAccessTokenOrIdToken(bearer);
    // Setting UserData
    UserValidator.validateUpdate(accessTokenData, userToUpdate);
    return dataService
        .updateUser(userToUpdate)
        .flatMap(
            user -> {
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "User " + user.getUsername() + " successfully updated!",
                      CorrelationIdUtils.getCorrelationId(),
                      user);
              return Mono.just(ResponseEntity.ok(response));
            });
  }
}
