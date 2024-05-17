package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
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

import java.net.URI;
import java.util.Base64;

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
      String basicOrBearer,
      ServerHttpRequest request) {
    return login(clientId, scope, code, prompt, null, includeUserInfo, basicOrBearer, request);
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
      String basicOrBearer,
      ServerHttpRequest request) {
    OAuthValidator.validateOAuthToken(clientId, grant_type);
    return login(
        clientId, scope, code, prompt, redirect_uri, includeUserInfo, basicOrBearer, request);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  private Mono<ResponseEntity<?>> login(
      String clientId,
      String scope,
      String code,
      String prompt,
      String redirect_uri,
      boolean includeUserInfo,
      String basicOrBearer,
      ServerHttpRequest request) {
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.flatMap(
        clientCredential -> {
          if (!ObjectUtils.isEmpty(basicOrBearer)
              && basicOrBearer.contains(OAuthType.BEARER.type())
              && ObjectUtils.isEmpty(code)) return validateBearer(basicOrBearer, includeUserInfo);
          switch (clientCredential.getAuthType()) {
            case BEARER -> {
              OAuthValidator.validateBasic(basicOrBearer);
              return makeClassicLogin(basicOrBearer, clientCredential, includeUserInfo);
            }
            case GOOGLE -> {
              LOG.info("Google oAuth 2.0 Login started");
              String redirectUri = redirect_uri;
              if (ObjectUtils.isEmpty(redirectUri))
                redirectUri = Utils.getCookie(COOKIE_REDIRECT_URI, request);
              OAuthValidator.validateOAuthGoogle(clientCredential, code, scope, redirectUri);
              GoogleModel googleModel =
                  grpcService.authenticateOAuth(code, scope, redirectUri, clientCredential);
              return performGoogleLogin(googleModel, clientCredential, includeUserInfo, request)
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

  private Mono<ResponseEntity<Response>> validateBearer(String bearer, boolean includeUserInfo) {
    return Mono.empty();
  }

  private Mono<ResponseEntity<Response>> performGoogleLogin(
      GoogleModel googleModel,
      ClientCredential clientCredential,
      boolean includeUserInfo,
      ServerHttpRequest request) {
    return dataService
        .getUserByEmail(googleModel.getUserInfo().getEmail())
        .map(
            user -> {
              AuthToken token =
                  tokenService.generateToken(
                      user,
                      clientCredential,
                      Utils.putGoogleClaimsIntoToken(
                          clientCredential,
                          TokenClaims.GOOGLE_TOKEN,
                          googleModel.getTokenResponse()));
              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      "Login successfully, welcome " + user.getUsername() + " !",
                      CorrelationIdUtils.getCorrelationId(),
                      includeUserInfo
                          ? new OAuthTokenResponse(token, googleModel.getTokenResponse(), user)
                          : new OAuthTokenResponse(token, googleModel.getTokenResponse()));
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
                          AuthToken token =
                              tokenService.generateToken(
                                  user1,
                                  clientCredential,
                                  Utils.putGoogleClaimsIntoToken(
                                      clientCredential,
                                      TokenClaims.GOOGLE_TOKEN,
                                      googleModel.getTokenResponse()));
                          Response response =
                              new Response(
                                  HttpStatus.OK.value(),
                                  "Login successfully, welcome " + user1.getUsername() + " !",
                                  CorrelationIdUtils.getCorrelationId(),
                                  includeUserInfo
                                      ? new OAuthTokenResponse(
                                          token, googleModel.getTokenResponse(), user1)
                                      : new OAuthTokenResponse(
                                          token, googleModel.getTokenResponse()));
                          return ResponseEntity.ok(response);
                        });
              }
              return Mono.error(throwable);
            });
  }

  private Mono<ResponseEntity<Response>> makeClassicLogin(
      String basic, ClientCredential clientCredential, boolean includeUserInfo) {
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
        .login(username, email, password, clientCredential)
        .map(
            tokenResponse -> {
              String message =
                  "Login Successfully! Welcome back " + tokenResponse.getUser().getUsername() + "!";

              Response response =
                  new Response(
                      HttpStatus.OK.value(),
                      message,
                      CorrelationIdUtils.getCorrelationId(),
                      includeUserInfo
                          ? tokenResponse
                          : new OAuthTokenResponse(
                              tokenResponse.getToken(), tokenResponse.getStrapiJwt()));
              LOG.info("Login process ended for user {}", tokenResponse.getUser().getUsername());
              return ResponseEntity.ok(response);
            });
  }
}
