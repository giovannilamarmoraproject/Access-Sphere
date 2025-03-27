package io.github.giovannilamarmora.accesssphere.api.strapi;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiError;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.ExceptionType;
import io.github.giovannilamarmora.utils.exception.UtilsException;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import io.github.giovannilamarmora.utils.webClient.WebClientException;
import lombok.Getter;

public class StrapiException extends UtilsException {

  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_STRAPI_404;

  public StrapiException(String message) {
    super(DEFAULT_CODE, message);
  }

  public StrapiException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }

  public StrapiException(ExceptionCode exceptionCode, ExceptionType exception, String message) {
    super(exceptionCode, exception, message);
  }

  public static void handleStrapiException(Throwable throwable) {
    if (throwable instanceof WebClientException) {
      String errorMessage = ((WebClientException) throwable).getExceptionMessage();
      if (ObjectToolkit.isInstanceOf(errorMessage, new TypeReference<StrapiError>() {})) {
        StrapiError strapiError = Mapper.readObject(errorMessage, StrapiError.class);
        /** Login Error */
        if (strapiError
            .getError()
            .getMessage()
            .contains(StrapiErrorMessage.INVALID_EMAIL_PASS.getMessage())) {
          LOG.error(
              "Username o password are wrong, error is {}", strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
        } else if (strapiError
            .getError()
            .getMessage()
            .contains(StrapiErrorMessage.ACCOUNT_BLOCKED.getMessage())) {
          LOG.error("Account is blocked, error is {}", strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_423, ExceptionMap.ERR_OAUTH_423.getMessage());
          /** Registration Error */
        } else if (strapiError
            .getError()
            .getMessage()
            .equalsIgnoreCase(StrapiErrorMessage.EMAIL_ALREADY_USED.getMessage())) {
          LOG.error(
              "An error happen during registration on strapi, message is {}",
              strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_400,
              io.github.giovannilamarmora.accesssphere.exception.ExceptionType.USERNAME_EMAIL_TAKEN,
              strapiError.getError().getMessage());
        } else if (strapiError
            .getError()
            .getMessage()
            .equalsIgnoreCase(StrapiErrorMessage.UNIQUE_ATTRIBUTE.getMessage())) {
          String attribute =
              String.join(
                  ", ", strapiError.getError().getDetails().getErrors().getFirst().getPath());
          LOG.error(
              "An error happen during registration on strapi, message is {} for fields {}",
              strapiError.getError().getMessage(),
              attribute);
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_400,
              "The current attributes ".concat(attribute).concat(" must be unique."));
          /** User info */
        } else if (strapiError
            .getError()
            .getMessage()
            .equalsIgnoreCase(StrapiErrorMessage.INVALID_CREDENTIAL.getMessage())) {
          LOG.error(
              "Basic token is wrong or not valid, error is {}",
              strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
          /** Refresh Token */
        } else if (strapiError
            .getError()
            .getMessage()
            .equalsIgnoreCase(StrapiErrorMessage.REFRESH_TOKEN_NOT_FOUND.getMessage())) {
          LOG.error(
              "Refresh token is wrong or not valid, error is {}",
              strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
          /** User Update */
        } else if (strapiError
            .getError()
            .getMessage()
            .equalsIgnoreCase(StrapiErrorMessage.USER_NOT_FOUND.getMessage())) {
          LOG.error("User not found, error is {}", strapiError.getError().getMessage());
          throw new OAuthException(
              ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
        }
      }
    }
  }

  @Getter
  public enum StrapiErrorMessage {
    INVALID_EMAIL_PASS("Invalid identifier or password"),
    EMAIL_ALREADY_USED("Email or Username are already taken"),
    UNIQUE_ATTRIBUTE("This attribute must be unique"),
    INVALID_CREDENTIAL("Missing or invalid credentials"),
    REFRESH_TOKEN_NOT_FOUND("Refresh Token not found"),
    USER_NOT_FOUND("NotFoundError"),
    ACCOUNT_BLOCKED("Your account has been blocked by an administrator");

    private final String message;

    StrapiErrorMessage(String message) {
      this.message = message;
    }
  }
}
