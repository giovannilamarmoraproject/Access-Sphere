package io.github.giovannilamarmora.accesssphere.data.tech;

import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.UtilsException;

public class TechnicalException extends UtilsException {

  public TechnicalException(ExceptionCode exceptionCode) {
    super(exceptionCode);
  }

  public TechnicalException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }
}
