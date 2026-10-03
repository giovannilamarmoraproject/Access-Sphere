package io.github.giovannilamarmora.accesssphere.email.exception;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.utils.exception.ExceptionCode;
import io.github.giovannilamarmora.utils.exception.UtilsException;

/**
 * Eccezione specifica per anomalie verificatesi durante le operazioni di trasporto e invio email.
 */
public class EmailException extends UtilsException {

  private static final ExceptionCode DEFAULT_CODE = ExceptionMap.ERR_EMAIL_SEND_001;

  public EmailException(String message) {
    super(DEFAULT_CODE, message);
  }

  public EmailException(ExceptionCode exceptionCode, String message) {
    super(exceptionCode, message);
  }

  public EmailException(ExceptionCode exceptionCode, String message, String exceptionMessage) {
    super(exceptionCode, message, exceptionMessage);
  }
}
