package io.github.giovannilamarmora.accesssphere.mfa;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Utilità per la generazione e codifica di credenziali crittografiche MFA (chiavi TOTP e codici OTP email).
 */
@Component
@Logged
public class MFAUtils {

  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /**
   * Genera una chiave segreta Base32 casuale per l'autenticazione TOTP RFC 6238.
   *
   * @return Stringa segreta Base32.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String generateSecret() {
    GoogleAuthenticator gAuth = new GoogleAuthenticator();
    GoogleAuthenticatorKey key = gAuth.createCredentials();
    return key.getKey();
  }

  /**
   * Genera un codice OTP numerico a 6 cifre crittograficamente sicuro per canali come Email o SMS.
   *
   * @return Stringa di 6 cifre formattata con zero iniziali (es. "042918").
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String generateEmailOtpCode() {
    int code = SECURE_RANDOM.nextInt(1_000_000);
    return String.format("%06d", code);
  }

  /**
   * Formatta la chiave di sicurezza memorizzata per i codici OTP via email combinando il codice
   * e il timestamp di scadenza in millisecondi epoch.
   *
   * @param otpCode Codice a 6 cifre generato.
   * @param expiryMillis Timestamp millisecondi epoch in cui il codice scadrà.
   * @return Stringa serializzata nel formato "codice:scadenza".
   */
  public static String formatEmailOtpSecret(String otpCode, long expiryMillis) {
    return otpCode + ":" + expiryMillis;
  }

  /**
   * Compone l'URI standard 'otpauth://' per la configurazione automatica di app authenticator tramite QR code.
   *
   * @param userIdentifier Identificativo o email dell'utente.
   * @param secret Chiave segreta Base32.
   * @param issuer Nome dell'emittente (es. "Access-Sphere").
   * @return URI formattato secondo la specifica Key Uri Format.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String getOtpAuthUrl(String userIdentifier, String secret, String issuer) {
    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, userIdentifier, secret, issuer);
  }
}
