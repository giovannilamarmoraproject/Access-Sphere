package io.github.giovannilamarmora.accesssphere.mfa.strategy;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.mfa.MFAException;
import io.github.giovannilamarmora.accesssphere.mfa.MFAMapper;
import io.github.giovannilamarmora.accesssphere.mfa.MFAUtils;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupDataResponse;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFASetupRequest;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAMethod;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFASetting;
import io.github.giovannilamarmora.accesssphere.mfa.model.MFAType;
import io.github.giovannilamarmora.accesssphere.user.UserService;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.accesssphere.utilities.QRCodeUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Base64;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Strategia di autenticazione a due fattori basata su Time-Based One-Time Password (TOTP) RFC 6238.
 *
 * <p>Genera una chiave crittografica Base32 e l'URI compatibile con Google Authenticator,
 * Microsoft Authenticator e altri client conformi, con supporto opzionale alla generazione di QR Code in Base64.
 */
@Service
@Logged
public class TotpStrategy implements MFAStrategy {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private UserService userService;

  /**
   * Genera una chiave segreta TOTP per l'utente, predispone l'URI per il QR Code e aggiorna l'anagrafica utente con il metodo pendente.
   *
   * @param user Utente per il quale inizializzare l'MFA TOTP.
   * @param setupRequest Richiesta di setup con flag di generazione immagine QR code.
   * @return {@link Mono} contenente {@link MFASetupDataResponse}.
   */
  @Override
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public Mono<MFASetupDataResponse> generateSecret(User user, MFASetupRequest setupRequest) {
    LOG.info(
        "🔑 [TOTP] Generate TOTP MFA for user: {} process started.", setupRequest.identifier());
    String secret = MFAUtils.generateSecret();
    String otpAuth = MFAUtils.getOtpAuthUrl(user.getEmail(), secret, "Access-Sphere");

    MFASetting mfaSetting = MFAMapper.generateTempMFA(user, setupRequest, secret);

    user.setMfaSettings(mfaSetting);
    return userService
        .updateUser(setupRequest.identifier(), user)
        .map(
            userUpdated -> {
              MFASetupDataResponse mfaSetupResponse = new MFASetupDataResponse(secret, otpAuth, null);
              if (setupRequest.generateImage()) {
                String base64 =
                    "data:image/png;base64,"
                        + Base64.getEncoder()
                            .encodeToString(
                                QRCodeUtils.generateQRCodeWithDefaultLogo(otpAuth, 300, 300));
                mfaSetupResponse = mfaSetupResponse.addQRCode(base64);
              }
              return mfaSetupResponse;
            })
        .doOnSuccess(
            response ->
                LOG.info(
                    "✅ [TOTP] Generate TOTP MFA for user: {} process ended.",
                    setupRequest.identifier()));
  }

  /**
   * Verifica la correttezza del codice numerico a 6 cifre rispetto alla chiave segreta TOTP attiva.
   *
   * @param mfaMethods Metodi registrati per l'utente.
   * @param otp Codice a 6 cifre fornito dall'applicazione.
   * @param identifier Identificativo dell'utente per logging ed eccezioni.
   */
  @Override
  public void verifyCode(List<MFAMethod> mfaMethods, String otp, String identifier) {
    GoogleAuthenticator gAuth = new GoogleAuthenticator();

    boolean atLeastOneValid =
        mfaMethods.stream()
            .filter(mfaMethod -> mfaMethod.getType().equals(MFAType.TOTP))
            .anyMatch(
                method -> {
                  try {
                    return gAuth.authorize(method.getSecretKey(), Integer.parseInt(otp.trim()));
                  } catch (Exception e) {
                    LOG.warn(
                        "⚠️ OTP verification failed for method {} - {}",
                        method.getType(),
                        e.getMessage());
                    return false;
                  }
                });

    if (!atLeastOneValid) {
      LOG.error("❌ Invalid TOTP OTP code for all MFA methods for user: {}", identifier);
      throw new MFAException(
          ExceptionMap.ERR_MFA_400, ExceptionType.INVALID_OTP_CODE, "Invalid OTP code");
    }

    LOG.info("✅ TOTP OTP code verified successfully for user: {}", identifier);
  }
}
