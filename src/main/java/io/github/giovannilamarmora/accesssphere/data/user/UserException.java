package io.github.giovannilamarmora.accesssphere.data.user;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.UtilsException;

public class UserException extends UtilsException {
  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_OAUTH_500;

  public UserException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }
}
