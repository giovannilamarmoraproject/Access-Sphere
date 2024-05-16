package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.client.ClientService;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.DataService;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.data.user.dto.UserRole;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.correlationID.CorrelationIdUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Service
public class UserService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private DataService dataService;
  @Autowired private ClientService clientService;

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
          user.setRole(UserRole.USER);

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
          // user.setPassword(new String(Base64.getDecoder().decode(user.getPassword())));

          if (!Utils.checkCharacterAndRegexValid(
              user.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
            LOG.error("Invalid regex for field password for user {}", user.getUsername());
            throw new UserException(
                ExceptionMap.ERR_USER_400, ExceptionMap.ERR_USER_400.getMessage());
          }

          if (!Utils.checkCharacterAndRegexValid(user.getEmail(), RegEx.EMAIL.getValue())) {
            LOG.error("Invalid regex for field email for user {}", user.getUsername());
            throw new UserException(
                ExceptionMap.ERR_USER_400, ExceptionMap.ERR_USER_400.getMessage());
          }

          return dataService
              .registerUser(user)
              .map(
                  user1 -> {
                    Response response =
                        new Response(
                            HttpStatus.OK.value(),
                            "User " + user.getUsername() + " successfully registered!",
                            CorrelationIdUtils.getCorrelationId(),
                            user1);
                    return ResponseEntity.ok(response);
                  });
        });
  }
}
