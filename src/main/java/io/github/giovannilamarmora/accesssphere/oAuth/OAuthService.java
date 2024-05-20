package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleModel;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleOAuthMapper;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthTokenResponse;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import java.net.URI;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
@Logged
@RequiredArgsConstructor
public class OAuthService {
  private static final String COOKIE_TOKEN = "REGISTRATION-TOKEN";
  private static final String COOKIE_REDIRECT_URI = "REDIRECT-URI";

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  @Autowired private UserDataService userDataService;
  @Autowired private TokenService tokenService;
  @Autowired private ClientService clientService;
  @Autowired private DataService dataService;
  @Autowired private GrpcService grpcService;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<ResponseEntity<?>> authorize(
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
          OAuthValidator.validateClient(clientCredential, accessType, redirectUri, scope);
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
      String basic,
      ServerHttpRequest request) {
    return login(
        clientId, scope, code, prompt, null, includeUserInfo, includeUserData, basic, request);
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
    OAuthValidator.validateOAuthToken(clientId, grant_type);
    return login(
        clientId,
        scope,
        code,
        prompt,
        redirect_uri,
        includeUserInfo,
        includeUserData,
        basic,
        request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> login(
      String clientId,
      String scope,
      String code,
      String prompt,
      String redirect_uri,
      boolean includeUserInfo,
      boolean includeUserData,
      String basic,
      ServerHttpRequest request) {
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          switch (clientCredential.getAuthType()) {
            case BEARER -> {
              OAuthValidator.validateBasic(basic);
              return makeClassicLogin(
                  basic, clientCredential, includeUserInfo, includeUserData, request);
            }
            case GOOGLE -> {
              LOG.info("Google oAuth 2.0 Login started");
              String redirectUri = redirect_uri;
              if (ObjectUtils.isEmpty(redirectUri))
                redirectUri = Utils.getCookie(COOKIE_REDIRECT_URI, request);
              OAuthValidator.validateOAuthGoogle(clientCredential, code, scope, redirectUri);
              GoogleModel googleModel =
                  grpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
              return performGoogleLogin(
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

  private Mono<ResponseEntity<Response>> performGoogleLogin(
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
                  .equalsIgnoreCase(ExceptionMap.ERR_OAUTH_404.getMessage())) {
                String registration_token = Utils.getCookie(COOKIE_TOKEN, request);
                if (ObjectUtils.isEmpty(registration_token)) {
                  LOG.error("Missing registration_token");
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
                }
                if (!registration_token.equalsIgnoreCase(clientCredential.getRegistrationToken())) {
                  LOG.error("Invalid registration_token");
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
                }
                User userGoogle = GoogleOAuthMapper.generateGoogleUser(googleModel);
                userGoogle.setPassword(Utils.getCookie(COOKIE_TOKEN, request));
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
                          user1.setRoles(
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

  private Mono<ResponseEntity<Response>> makeClassicLogin(
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
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
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
                          tokenResponse.getStrapiJwt(),
                          includeUserInfo ? tokenResponse.getUserInfo() : null,
                          includeUserData ? tokenResponse.getUser() : null));
              LOG.info("Login process ended for user {}", tokenResponse.getUser().getUsername());
              return ResponseEntity.ok(response);
            });
  }
}
