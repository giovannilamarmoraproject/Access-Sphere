package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.RegEx;
import io.github.giovannilamarmora.utils.generic.Response;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Mono;

@Component
public class UserValidator {

  private static final Logger LOG = LoggerFilter.getLogger(UserValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateAuthType(
      ClientCredential clientCredential, AccessTokenData accessTokenData) {
    if (!clientCredential.getAuthType().equals(OAuthType.ALL_TYPE))
      if (!clientCredential.getAuthType().equals(accessTokenData.getType())) {
        LOG.error("Invalid Authentication Type on client");
        throw new OAuthException(
            ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
      }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateGoogleUserInfo(GoogleModel userInfo, JWTData decryptToken) {
    if (!userInfo.getUserInfo().getEmail().equalsIgnoreCase(decryptToken.getEmail())) {
      LOG.error(
          "The JWT User {}, is different than the one on google {}",
          decryptToken.getEmail(),
          userInfo.getUserInfo().get("email"));
      throw new OAuthException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static Mono<ResponseEntity<Response>> defaultErrorOnType() {
    LOG.error("Type miss match on client");
    return Mono.error(
        new OAuthException(ExceptionMap.ERR_OAUTH_400, ExceptionMap.ERR_OAUTH_400.getMessage()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateRegistration(
      String registration_token, ClientCredential clientCredential, User user) {
    if (!Utilities.isCharacterAndRegexValid(user.getPassword(), RegEx.PASSWORD_FULL.getValue())) {
      LOG.error("Invalid regex for field password for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid password pattern!");
    }

    if (!Utilities.isCharacterAndRegexValid(user.getEmail(), RegEx.EMAIL.getValue())) {
      LOG.error("Invalid regex for field email for user {}", user.getUsername());
      throw new UserException(ExceptionMap.ERR_USER_400, "Invalid email provided!");
    }

    if (ObjectUtils.isEmpty(registration_token)) {
      LOG.error("Missing registration_token");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Missing registration_token!");
    }
    if (!registration_token.equalsIgnoreCase(clientCredential.getRegistrationToken())) {
      LOG.error("Invalid registration_token");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid registration_token!");
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUpdate(AccessTokenData accessTokenData, User userToUpdate) {
    if (!accessTokenData.getIdentifier().equalsIgnoreCase(userToUpdate.getIdentifier())) {
      LOG.error(
          "Identifier miss match, you should use {} instead of {}",
          accessTokenData.getIdentifier(),
          userToUpdate.getIdentifier());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_400, "Invalid identifier!");
    }
  }
}
