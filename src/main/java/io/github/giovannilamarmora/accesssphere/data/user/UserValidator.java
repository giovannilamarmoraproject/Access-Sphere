package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Component
public class UserValidator {

  private static final Logger LOG = LoggerFilter.getLogger(UserValidator.class);

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

  /**
   * Convalida i dati anagrafici e il rispetto dei vincoli di regex per password ed email.
   *
   * @param user DTO utente da validare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUserData(User user) {
    if (user == null) {
      LOG.error("User payload is null");
      throw new UserException(ExceptionMap.ERR_USER_400, "User data is required!");
    }
    if (ObjectUtils.isEmpty(user.getUsername())) {
      LOG.error("Username is empty");
      throw new UserException(ExceptionMap.ERR_USER_400, "Username is required!");
    }
    if (!Utilities.isCharacterAndRegexValid(user.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("Invalid regex for field password for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
    }
    if (!Utilities.isCharacterAndRegexValid(user.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("Invalid regex for field email for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid email provided!");
    }
  }

  /**
   * Convalida che il client specificato consenta la registrazione degli utenti (blocco preventivo).
   *
   * @param oAuth2Client Il client da verificare.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
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

  @Deprecated(since = "2.0.0")
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRegistration(
      String registration_token, OAuth2Client oAuth2Client, User user) {
    validateUserData(user);
    validateClientRegistrationEnabled(oAuth2Client);
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
  public static void validateUser(
      io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity userEntity) {
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
