package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.api.strapi.StrapiService;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.GrpcService;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleModel;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleOAuthMapper;
import io.github.giovannilamarmora.accesssphere.grpc.google.GoogleOAuthService;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
  public Mono<?> login(
      String clientId,
      String scope,
      String code,
      String prompt,
      String basic,
      ServerHttpRequest request) {
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          switch (clientCredential.getAuthType()) {
            case BEARER -> {
              OAuthValidator.validateBasic(basic);
              return Mono.just(ResponseEntity.ok(makeClassicLogin(basic, clientCredential)));
            }
            case GOOGLE -> {
              LOG.info("Google oAuth 2.0 Login started");
              OAuthValidator.validateCode(code);
              GoogleModel googleModel =
                  grpcService.authenticateOAuth(
                      code, scope, Utils.getCookie(COOKIE_REDIRECT_URI, request), clientCredential);
              return performGoogleLogin(googleModel, clientCredential, request)
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
      GoogleModel googleModel, ClientCredential clientCredential, ServerHttpRequest request) {
    return dataService
        .getUserByEmail(googleModel.getUserInfo().getEmail())
        .map(
            user -> {
              user.setAuthToken(
                  tokenService.generateToken(
                      user,
                      clientCredential,
                      putClaims(clientCredential, googleModel.getTokenResponse())));
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Login successfully, welcome " + user.getUsername() + " !",
                      CorrelationIdUtils.getCorrelationId(),
                      user);
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
                User userGoogle = GoogleOAuthMapper.generateGoogleUser(googleModel.getUserInfo());
                userGoogle.setPassword(Utils.getCookie(COOKIE_TOKEN, request));
                return dataService
                    .registerUser(userGoogle)
                    .map(
                        user1 -> {
                          user1.setAuthToken(
                              tokenService.generateToken(
                                  user1,
                                  clientCredential,
                                  putClaims(clientCredential, googleModel.getTokenResponse())));
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Login successfully, welcome " + user1.getUsername() + " !",
                                  CorrelationIdUtils.getCorrelationId(),
                                  user1);
                          return ResponseEntity.ok(response);
                        });
              }
              return Mono.error(throwable);
            });
  }

  private ResponseEntity<Response> makeClassicLogin(
      String basic, ClientCredential clientCredential) {
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

    UserEntity userEntity = userDataService.findUserEntityByUsernameOrEmail(username, email);

    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("An error happen during findUserEntityByUsernameOrEmail()");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }

    boolean matches = bCryptPasswordEncoder.matches(password, userEntity.getPassword());
    if (!matches) {
      LOG.error(
          "An error happen during findUserEntityByUsernameOrEmail(), the password do not match");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    User userEntityToUser = UserMapper.mapUserEntityToUser(userEntity);
    userEntityToUser.setPassword(null);
    userEntityToUser.setAuthToken(
        tokenService.generateToken(userEntityToUser, clientCredential, null));

    String message = "Login Successfully! Welcome back " + username + "!";

    Response response =
        new Response(
            HttpStatus.OK.value(),
            message,
            CorrelationIdUtils.getCorrelationId(),
            userEntityToUser);
    LOG.debug("Login process ended for user {}", username);
    return ResponseEntity.ok(response);
  }

  private Map<String, Object> putClaims(
      ClientCredential clientCredential, TokenResponse googleTokenResponse) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(TokenClaims.AUTH_TYPE.claim(), clientCredential.getAuthType());
    try {
      attributes.put(
          TokenClaims.GOOGLE_TOKEN.claim(), mapper.writeValueAsString(googleTokenResponse));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error happen during oAuth Google Login on parsing User, message is {}",
          e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    return attributes;
  }
}
