package io.github.giovannilamarmora.accesssphere.utilities;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.token.model.TokenClaims;
import io.github.giovannilamarmora.utils.context.ContextConfig;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.web.CookieManager;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.util.ObjectUtils;

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
    attributes.put(TokenClaims.GOOGLE_TOKEN.claim(), Mapper.writeObjectToString(data));
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
        if (!ObjectUtils.isEmpty(replacement)) matcher.appendReplacement(result, replacement);
      }
      matcher.appendTail(result);

      finalParam.put(key, result.toString());
    }
    return finalParam;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String encodeURLValue(String value) {
    return ObjectUtils.isEmpty(value) ? value : URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String decodeURLValue(String value) {
    return ObjectUtils.isEmpty(value) ? value : URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  public static <E extends Enum<E>> boolean isEnumValue(String value, Class<E> enumClass) {
    try {
      Enum.valueOf(enumClass, value);
      return true;
    } catch (IllegalArgumentException | NullPointerException e) {
      return false;
    }
  }

  public static void deleteAllCookie(ServerHttpResponse response) {
    CookieManager.deleteCookie(Cookie.COOKIE_SESSION_ID, response);
    CookieManager.deleteCookie(Cookie.COOKIE_TOKEN, response);
    CookieManager.deleteCookie(Cookie.COOKIE_REDIRECT_URI, response);
    CookieManager.deleteCookie(Cookie.COOKIE_ACCESS_TOKEN, response);
    CookieManager.deleteCookie(Cookie.COOKIE_STRAPI_TOKEN, response);
    CookieManager.deleteCookie(ContextConfig.TRACE_ID.getValue(), response);
    CookieManager.deleteCookie(ContextConfig.SPAN_ID.getValue(), response);
    CookieManager.deleteCookie(ContextConfig.PARENT_ID.getValue(), response);
  }
}
