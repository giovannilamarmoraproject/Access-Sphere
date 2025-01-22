package io.github.giovannilamarmora.accesssphere.token;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.UtilsException;

public class TokenException extends UtilsException {
  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_TOKEN_500;

  public TokenException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }

  public TokenException(String message) {
    super(DEFAULT_CODE, message);
  }
}
