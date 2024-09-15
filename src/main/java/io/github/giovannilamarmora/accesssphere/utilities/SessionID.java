package io.github.giovannilamarmora.accesssphere.utilities;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.util.Random;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.springframework.http.server.reactive.ServerHttpResponse;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionID {

  private String sessionID;

  private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private static final Logger LOG = LoggerFilter.getLogger(SessionID.class);

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder {
    private int letterCount = 4;
    private int uuidPartLength = 8;

    public Builder withLetterCount(int count) {
      this.letterCount = count;
      return this;
    }

    public Builder withUUIDPartLength(int length) {
      this.uuidPartLength = length;
      return this;
    }

    @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
    public String generate() {
      // 1. Genera lettere casuali
      String randomLetters = generateRandomLetters(letterCount);

      // 2. Ottieni il timestamp corrente
      long timestamp = System.currentTimeMillis();

      // 3. Genera una stringa casuale da UUID, limitata a uuidPartLength caratteri
      String randomUUIDPart = generateRandomUUIDPart(uuidPartLength).toUpperCase();

      // 4. Combina i componenti nel formato desiderato
      return String.format("%s-%d-%s", randomLetters, timestamp, randomUUIDPart);
    }

    private String generateRandomLetters(int length) {
      Random random = new Random();
      StringBuilder sb = new StringBuilder(length);
      for (int i = 0; i < length; i++) {
        sb.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
      }
      return sb.toString();
    }

    private String generateRandomUUIDPart(int length) {
      String uuid = UUID.randomUUID().toString().replace("-", "");
      return uuid.substring(0, Math.min(length, uuid.length()));
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public static void invalidateSessionID(ServerHttpResponse response) {
    CookieManager.deleteCookie(Cookie.COOKIE_SESSION_ID, response);
    LOG.info("Session ID successfully invalidated");
  }
}
