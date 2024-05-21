package io.github.giovannilamarmora.accesssphere.utilities;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.token.dto.TokenClaims;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ObjectUtils;

public class Utils {

  private static final ObjectMapper mapper =
      new ObjectMapper()
          .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
          .findAndRegisterModules();

  private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

  public static ObjectMapper mapper() {
    return mapper;
  }

  public static boolean checkCharacterAndRegexValid(String field, String regex) {
    if (ObjectUtils.isEmpty(field) || ObjectUtils.isEmpty(regex)) return false;
    Pattern p = Pattern.compile(regex);
    Matcher m = p.matcher(field);
    return m.find();
  }

  public static <T> boolean isInstanceOf(String source, TypeReference<T> typeReference) {
    try {
      return !isNullOrEmpty(mapper.readValue(source, typeReference));
    } catch (JsonProcessingException e) {
      return false;
    }
  }

  public static boolean isNullOrEmpty(Object obj) {
    if (ObjectUtils.isEmpty(obj)) return true;
    // Ottiene tutti i campi della classe dell'oggetto
    Field[] campi = obj.getClass().getDeclaredFields();
    // Itera su tutti i campi
    for (Field campo : campi) {
      campo.setAccessible(true); // Permette l'accesso ai campi privati
      try {
        // Controlla se il campo è null
        if (campo.get(obj) != null) {
          return false; // Se anche solo un campo non è null, restituisce false
        }
      } catch (IllegalAccessException e) {
        return true;
      }
    }
    return true; // Se tutti i campi sono null, restituisce true
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static HttpHeaders setCookieInResponse(String cookieName, String cookieValue) {
    LOG.info("Setting Cookie {}, with value {}", cookieName, cookieValue);
    ResponseCookie cookie =
        ResponseCookie.from(cookieName, cookieValue)
            .maxAge(360000L)
            .sameSite("None")
            .secure(true)
            .httpOnly(true)
            .path("/")
            .build();
    HttpHeaders headers = new HttpHeaders();
    headers.add(HttpHeaders.SET_COOKIE, cookie.toString());
    return headers;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.UTILS_LOGGER)
  public static String getCookie(String cookieName, ServerHttpRequest request) {
    LOG.info("Getting Cookie {}", cookieName);
    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
    if (ObjectUtils.isEmpty(cookies) || ObjectUtils.isEmpty(cookies.get(cookieName))) return null;
    return Objects.requireNonNull(cookies.get(cookieName)).getFirst().getValue();
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
}
