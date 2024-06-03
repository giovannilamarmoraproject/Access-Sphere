package io.github.giovannilamarmora.accesssphere.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;

public class Utils {

  private static final ObjectMapper mapper =
      new ObjectMapper()
          .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
          .findAndRegisterModules();

  private static final Logger LOG = LoggerFilter.getLogger(Utils.class);

  public static ObjectMapper mapper() {
    return mapper;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static Map<String, Object> putGoogleClaimsIntoToken(
      ClientCredential clientCredential, TokenClaims claim, Object data) {
    Map<String, Object> attributes = new HashMap<>();
    attributes.put(claim.claim(), clientCredential.getAuthType());
    try {
      attributes.put(TokenClaims.GOOGLE_TOKEN.claim(), mapper.writeValueAsString(data));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error happen during oAuth Google Login on parsing User, message is {}",
          e.getMessage());
      throw new OAuthException(ExceptionMap.ERR_OAUTH_403, ExceptionMap.ERR_OAUTH_403.getMessage());
    }
    return attributes;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static Map<String, String> getFinalMapFromValue(
      Map<String, String> source, Map<String, String> target) {
    // Creazione della mappa finalParam
    Map<String, String> finalParam = new HashMap<>();
    Pattern pattern = Pattern.compile("\\{\\{(.+?)\\}\\}");

    for (Map.Entry<String, String> entry : target.entrySet()) {
      String key = entry.getKey();
      String value = entry.getValue();
      Matcher matcher = pattern.matcher(value);

      StringBuffer result = new StringBuffer();
      while (matcher.find()) {
        String placeholder = matcher.group(1);
        String replacement = source.getOrDefault(placeholder, matcher.group(0));
        matcher.appendReplacement(result, replacement);
      }
      matcher.appendTail(result);

      finalParam.put(key, result.toString());
    }
    return finalParam;
  }
}
