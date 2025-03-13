package io.github.giovannilamarmora.accesssphere.oAuth;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.ExceptionType;
import io.github.giovannilamarmora.utils.exception.UtilsException;

public class OAuthException extends UtilsException {
  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_OAUTH_500;

  public OAuthException(ExceptionCode exceptionCode) {
    super(exceptionCode);
  }

  public OAuthException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }

  public OAuthException(ExceptionCode exceptionCode, ExceptionType exception, String message) {
    super(exceptionCode, exception, message);
  }
}
