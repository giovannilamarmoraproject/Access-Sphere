package io.github.giovannilamarmora.accesssphere.mfa;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAConfirmationRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAManageRequest;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MfaVerificationRequest;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Validatore per le richieste e i parametri del modulo Multi-Factor Authentication (MFA).
 */
@Component
public class MFAValidator {

  private static final Logger LOG = LoggerFilter.getLogger(MFAValidator.class);

  /**
   * Convalida che il metodo MFA specificato dal client chiamante sia compreso tra quelli attesi.
   *
   * @param actual Metodo effettivo richiesto.
   * @param expected Lista dei metodi MFA abilitati o attesi per l'utente/sessione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateMFAMethods(MFAType actual, List<MFAType> expected) {
    if (ObjectToolkit.isNullOrEmpty(actual)) {
      LOG.error("❌ The request must have a valid MFA method to proceed");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_MFA_METHOD,
          "Invalid or missing MFA Method!");
    }

    if (expected == null || !expected.contains(actual)) {
      LOG.error("❌ MFA method {} is not valid. Expected methods: {}", actual, expected);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_MFA_METHOD,
          "Invalid MFA Method! Expected one of: " + expected);
    }
  }

  /**
   * Convalida il payload della richiesta di setup MFA.
   *
   * @param request Richiesta di setup.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateSetupRequest(MFASetupRequest request) {
    if (request == null) {
      LOG.error("❌ MFA setup payload cannot be null");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA setup payload cannot be null");
    }
    if (ObjectToolkit.isNullOrEmpty(request.identifier())) {
      LOG.error("❌ User identifier cannot be empty in MFA setup");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "User identifier is required");
    }
    if (request.type() == null) {
      LOG.error("❌ MFA type is required");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA type is required");
    }
    if (request.type() == MFAType.TOTP && request.label() == null) {
      LOG.error("❌ Authenticator app label is required for TOTP MFA setup");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, "MFA label/provider is required for TOTP setup");
    }
  }

  /**
   * Convalida il payload di conferma del codice OTP MFA.
   *
   * @param request Richiesta di conferma.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateConfirmationRequest(MFAConfirmationRequest request) {
    if (request == null) {
      LOG.error("❌ MFA confirmation payload cannot be null");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA confirmation payload cannot be null");
    }
    if (ObjectToolkit.isNullOrEmpty(request.identifier())) {
      LOG.error("❌ User identifier is required in MFA confirmation");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "User identifier is required");
    }
    validateOtpFormat(request.otp());
  }

  /**
   * Convalida il payload di verifica codice OTP durante il login.
   *
   * @param request Richiesta di verifica.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateVerificationRequest(MfaVerificationRequest request) {
    if (request == null) {
      LOG.error("❌ MFA verification payload cannot be null");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA verification payload cannot be null");
    }
    if (request.mfaMethod() == null) {
      LOG.error("❌ MFA method must be specified for verification");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_MFA_METHOD,
          "MFA method must be specified");
    }
    validateOtpFormat(request.otp());
  }

  /**
   * Convalida che il codice OTP sia una sequenza numerica valida a 6 cifre.
   *
   * @param otp Stringa del codice OTP.
   */
  public static void validateOtpFormat(String otp) {
    if (ObjectToolkit.isNullOrEmpty(otp) || !otp.trim().matches("^\\d{6}$")) {
      LOG.error("❌ Invalid OTP code format: expected exactly 6 digits");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400,
          ExceptionType.INVALID_OTP_CODE,
          "The OTP code must consist of exactly 6 digits");
    }
  }

  /**
   * Convalida la richiesta di gestione dei metodi MFA.
   *
   * @param request Richiesta di gestione.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateManageRequest(MFAManageRequest request) {
    if (request == null) {
      LOG.error("❌ MFA manage payload cannot be null");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA manage payload cannot be null");
    }
    if (ObjectToolkit.isNullOrEmpty(request.identifier())) {
      LOG.error("❌ User identifier is required in MFA manage");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "User identifier is required");
    }
    if (request.action() == null) {
      LOG.error("❌ Management action is required");
      throw new MFAException(ExceptionMap.ERR_MFA_400, "MFA Action is required");
    }
    if (request.action() == MFAManageRequest.Action.DELETE && request.label() == null) {
      LOG.error("❌ Method label is required when deleting an MFA method");
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, "Label is required when deleting an MFA method");
    }
  }
}
