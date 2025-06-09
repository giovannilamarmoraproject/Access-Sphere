package io.github.giovannilamarmora.accesssphere.utilities;

import java.security.SecureRandom;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public final class OneTimeCodeUtils {

  private static final SecureRandom RANDOM = new SecureRandom();

  private OneTimeCodeUtils() {}

  public static String generateNumericCode(int digits) {
    int bound = (int) Math.pow(10, digits);
    return String.format("%0" + digits + "d", RANDOM.nextInt(bound));
  }

  public static String hash(String code) {
    return new BCryptPasswordEncoder().encode(code);
  }

  public static boolean matches(String rawCode, String hash) {
    return new BCryptPasswordEncoder().matches(rawCode, hash);
  }
}
