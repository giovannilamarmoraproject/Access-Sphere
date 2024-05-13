package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.database.UserDataService;
import io.github.giovannilamarmora.accesssphere.data.user.UserMapper;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.google.GoogleOAuthService;
import io.github.giovannilamarmora.accesssphere.token.TokenService;
import io.github.giovannilamarmora.accesssphere.token.dto.AuthToken;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

import javax.swing.text.Utilities;
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

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private UserDataService userDataService;
  final BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
  @Autowired private TokenService tokenService;
  @Autowired private ClientService clientService;
  @Autowired private GoogleOAuthService googleOAuthService;
  private final ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();

  public Mono<ResponseEntity<?>> authorize(
      String accessType, String clientId, String redirectUri, String scope, String state) {
    LOG.info("Starting endpoint authorize with client id: {}", clientId);
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);

    return clientCredentialMono.map(
        clientCredential -> {
          OAuthValidator.validateClient(clientCredential, accessType, redirectUri, scope);
          try {
            String redirect =
                googleOAuthService.startGoogleAuthorization(
                    List.of(scope.split(" ")),
                    redirectUri,
                    accessType,
                    clientCredential.getExternalClientId(),
                    clientCredential.getClientSecret());
            URI location = new URI(redirect);
            LOG.info("Completed endpoint authorize with client id: {}", clientId);
            return ResponseEntity.status(HttpStatus.TEMPORARY_REDIRECT).location(location).build();
          } catch (IOException | GeneralSecurityException | URISyntaxException e) {
            LOG.error(
                "An error happen during oAuth Google Authorization, message is {}", e.getMessage());
            throw new OAuthException(
                ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
          }
        });
  }

  public Mono<?> login(String clientId, String scope, String code, String prompt, String basic) {
    Mono<ClientCredential> clientCredentialMono =
        clientService.getClientCredentialByClientID(clientId);
    // TODO: Validazione campi?

    return clientCredentialMono.map(
        clientCredential -> {
          // Controllo se è una login classica
          switch (clientCredential.getAuthType()) {
            case BEARER -> {
              if (ObjectUtils.isEmpty(basic)) {
                LOG.error("No basic auth found");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
              }
              return ResponseEntity.ok(makeClassicLogin(basic, clientCredential));
            }
            case GOOGLE -> {
              if (ObjectUtils.isEmpty(code)) {
                LOG.error("No Code found");
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage());
              }
              TokenResponse googleTokenResponse;
              try {
                googleTokenResponse =
                    googleOAuthService.getTokenResponse(
                        code,
                        clientCredential.getExternalClientId(),
                        clientCredential.getClientSecret(),
                        List.of(scope.split(" ")),
                        clientCredential.getAccessType().value(),
                        clientCredential.getRedirect_uri());
                // Devi salvare l'utente a DB se non esiste già, quindi capisci come prelevare le
                // informazioni.
                // Cerca per Email all'interno di strapi, se strapi non funziona vai nel database,
                // il
                // database per info utente e per clientid è secondario
                GoogleIdToken.Payload userInfo =
                    googleOAuthService.getUserInfo(
                        googleTokenResponse.get("id_token").toString(),
                        clientCredential.getExternalClientId());
                Map<String, Object> attributes = new HashMap<>();
                attributes.put(TokenClaims.AUTH_TYPE.claim(), clientCredential.getAuthType());
                attributes.put(
                    TokenClaims.GOOGLE_TOKEN.claim(),
                    mapper.writeValueAsString(googleTokenResponse));
                User user =
                    new User(
                        getUserInfoValue(userInfo, "given_name"),
                        getUserInfoValue(userInfo, "family_name"),
                        userInfo.getEmail(),
                        userInfo.getSubject(),
                        null,
                        UserRole.USER,
                        getUserInfoValue(userInfo, "picture"),
                        null,
                        getUserInfoValue(userInfo, "phone_number"),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        userInfo,
                        null);
                user.setAuthToken(tokenService.generateToken(user, clientCredential, attributes));
                Response response =
                    new Response(
                        HttpStatus.OK.value(),
                        "Login successfully, welcome " + user.getUsername() + " !",
                        CorrelationIdUtils.getCorrelationId(),
                        user);
                return ResponseEntity.ok(response);
              } catch (IOException | GeneralSecurityException e) {
                LOG.error(
                    "An error happen during oAuth Google Login, message is {}", e.getMessage());
                throw new OAuthException(
                    ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
              }
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

    Mono<UserEntity> userEntity = userDataService.findUserEntityByUsernameOrEmail(username, email);
    Mono<User> userMono =
        userEntity
            .map(
                user -> {
                  boolean matches = bCryptPasswordEncoder.matches(password, user.getPassword());
                  if (!matches) {
                    LOG.error(
                        "An error happen during findUserEntityByUsernameOrEmail(), the password do not match");
                    throw new OAuthException(
                        ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
                  }
                  User userEntityToUser = UserMapper.mapUserEntityToUser(user);
                  userEntityToUser.setPassword(null);
                  userEntityToUser.setAuthToken(
                      tokenService.generateToken(userEntityToUser, clientCredential, null));
                  return userEntityToUser;
                })
            .doOnError(
                throwable -> {
                  LOG.error(
                      "An error happen during findUserEntityByUsernameOrEmail(), message is {}",
                      throwable.getMessage());
                  throw new OAuthException(
                      ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
                });

    String message = "Login Successfully! Welcome back " + username + "!";

    Response response =
        new Response(
            HttpStatus.OK.value(), message, CorrelationIdUtils.getCorrelationId(), userMono);
    LOG.debug("Login process ended for user {}", username);
    return ResponseEntity.ok(response);
  }

  private String getUserInfoValue(GoogleIdToken.Payload userInfo, String value) {
    String toReturn =
        ObjectUtils.isEmpty(userInfo.get(value)) ? null : userInfo.get(value).toString();
    if (!ObjectUtils.isEmpty(toReturn)) userInfo.remove(value);
    return toReturn;
  }
}
