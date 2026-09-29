package io.github.giovannilamarmora.accesssphere.user;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleHolder;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.user.dto.UserRequest;
import io.github.giovannilamarmora.accesssphere.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Component
public class UserValidator {

  private static final Logger LOG = LoggerFilter.getLogger(UserValidator.class);

  /**
   * Convalida i dati anagrafici e il rispetto dei vincoli di regex per password ed email.
   *
   * @param user DTO utente da validare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUser(User user) {
    validateUsername(user.getUsername());
    validateEmail(user.getEmail());
    validateRoles(user.getRoles());
  }

  /**
   * Convalida i dati anagrafici e il rispetto dei vincoli di regex per password ed email.
   *
   * @param user DTO utente da validare.
   */
  public static void validateUserRequest(UserRequest user) {
    validateUsername(user.getUsername());
    validateEmail(user.getEmail());
    validatePassword(user.getUsername(), user.getPassword());
    validateRoles(user.getRoles());
  }

  private static void validateUsername(String username) {
    if (ObjectToolkit.isNullOrEmpty(username)) {
      LOG.error("Username is empty");
      throw new UserException(ExceptionMap.ERR_USER_400, "Username is required!");
    }
  }

  private static void validatePassword(String username, String password) {
    if (!Utilities.isCharacterAndRegexValid(password, RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("Invalid regex for field password for user {}", username);
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
    }
  }

  private static void validateEmail(String email) {
    if (!Utilities.isCharacterAndRegexValid(email, RegEx.EMAIL.getValue())) {
      LOG.error("Invalid regex for field email for user {}", email);
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid email provided!");
    }
  }

  private static void validateRoles(List<? extends RoleHolder> roles) {
    if (ObjectToolkit.isNullOrEmpty(roles)) {
      LOG.error("❌ Registration rejected: user roles are required");
      throw new UserException(ExceptionMap.ERR_USER_400, "User roles must be provided!");
    }
    roles.forEach(
        role -> {
          if (ObjectToolkit.isNullOrEmpty(role.getRole())) {
            LOG.error("❌ The Client must have a valid role configuration to proceed");
            throw new OAuthException(
                ExceptionMap.ERR_USER_400,
                ExceptionType.INVALID_ROLES,
                "Invalid Roles Configuration! At least one role is required.");
          }
          if (ObjectToolkit.isNullOrEmpty(role.getClientId())) {
            LOG.error("❌ The Client must have a valid client id configuration to proceed");
            throw new OAuthException(
                ExceptionMap.ERR_USER_400,
                ExceptionType.INVALID_ROLES,
                "Invalid Roles Configuration! Client id is required.");
          }
        });
  }

  /**
   * Convalida che il client specificato consenta la registrazione degli utenti (blocco preventivo).
   *
   * @param oAuth2Client Il client da verificare.
   */
  public static void validateClientRegistrationEnabled(OAuth2Client oAuth2Client) {
    if (oAuth2Client == null) {
      LOG.error("OAuth2Client is null");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid client_id provided!");
    }
    if (Boolean.FALSE.equals(oAuth2Client.getEnableRegistration())) {
      LOG.error("❌ Registration is disabled for client {}", oAuth2Client.getClientId());
      throw new OAuthException(
          ExceptionMap.ERR_OAUTH_403,
          "User registration is disabled for client " + oAuth2Client.getClientId());
    }
  }

  // TODO: OLD METHOD

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateAuthType(
      OAuth2Client oAuth2Client, OAuth2Authorization oAuth2Authorization) {
    if (!oAuth2Client.getAuthType().equals(OAuthType.ALL_TYPE))
      if (!oAuth2Client.getAuthType().equals(oAuth2Authorization.getType())) {
        LOG.error("❌ Invalid Authentication Type on client");
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
      }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateGoogleUserInfo(GoogleModel userInfo, JWTData decryptToken) {
    if (!userInfo.getUserInfo().getEmail().equalsIgnoreCase(decryptToken.getEmail())) {
      LOG.error(
          "❌ The JWT User {}, is different than the one on google {}",
          decryptToken.getEmail(),
          userInfo.getUserInfo().get("email"));
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }

  /**
   * Restituisce un errore reattivo standard in caso di mancata corrispondenza del tipo di
   * autenticazione client.
   *
   * @param <T> Tipo generico di ritorno per integrarsi con qualsiasi catena reattiva Mono.
   * @return Mono con errore {@link OAuthException} ERR_OAUTH_400.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static <T> Mono<T> defaultErrorOnType() {
    LOG.error("❌ Authentication type mismatch on client configuration");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUpdate(OAuth2Authorization oAuth2Authorization, User userToUpdate) {
    if (userToUpdate == null
        || userToUpdate.getIdentifier() == null
        || oAuth2Authorization == null
        || oAuth2Authorization.getIdentifier() == null
        || !oAuth2Authorization.getIdentifier().equalsIgnoreCase(userToUpdate.getIdentifier())) {
      LOG.error(
          "Identifier miss match, you should use {} instead of {}",
          oAuth2Authorization != null ? oAuth2Authorization.getIdentifier() : "null",
          userToUpdate != null ? userToUpdate.getIdentifier() : "null");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid identifier!");
    }
  }

  /**
   * Valida la presenza dell'entita' utente nel database.
   *
   * @param userEntity L'entita' da verificare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUser(UserEntity userEntity) {
    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("❌ User not found into database");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }
  }

  /**
   * Valida la validita' temporale del token di reset (massimo 24 ore).
   *
   * @param updateDate Data di aggiornamento o emissione del token.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateResetToken(java.time.LocalDateTime updateDate) {
    if (updateDate.plusDays(1).isBefore(java.time.LocalDateTime.now())) {
      LOG.error("⏳ Token Expired, last created was at {}", updateDate);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }

  /**
   * Valida la corrispondenza esatta tra l'identificatore fornito e quello atteso.
   *
   * @param actual_identifier Identificatore effettivo.
   * @param expected_identifier Identificatore atteso.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateIdentifier(String actual_identifier, String expected_identifier) {
    if (!actual_identifier.equalsIgnoreCase(expected_identifier)) {
      LOG.error(
          "⛔ Identifier validation failed: expected='{}', but got='{}'",
          expected_identifier,
          actual_identifier);
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }
}
