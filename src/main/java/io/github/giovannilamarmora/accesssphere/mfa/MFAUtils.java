package io.github.giovannilamarmora.accesssphere.mfa;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import org.springframework.stereotype.Component;

@Component
@Logged
public class MFAUtils {

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String generateSecret() {
    GoogleAuthenticator gAuth = new GoogleAuthenticator();
    GoogleAuthenticatorKey key = gAuth.createCredentials();
    return key.getKey(); // Restituisce il secret generato
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String getOtpAuthUrl(String userIdentifier, String secret, String issuer) {
    return String.format(
        "otpauth://totp/%s:%s?secret=%s&issuer=%s", issuer, userIdentifier, secret, issuer);
  }
}
