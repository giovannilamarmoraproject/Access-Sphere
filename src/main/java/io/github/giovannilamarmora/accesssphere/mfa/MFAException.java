package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.ExceptionType;
import io.github.giovannilamarmora.utils.exception.UtilsException;

public class MFAException extends UtilsException {
  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_MFA_400;

  public MFAException(ExceptionCode exceptionCode) {
    super(exceptionCode);
  }

  public MFAException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }

  public MFAException(ExceptionCode exceptionCode, ExceptionType exception, String message) {
    super(exceptionCode, exception, message);
  }
}
