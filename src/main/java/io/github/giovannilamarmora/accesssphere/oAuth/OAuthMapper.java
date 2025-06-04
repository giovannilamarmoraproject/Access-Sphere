package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.oAuth.model.GrantType;
import io.github.giovannilamarmora.accesssphere.oAuth.model.OAuthType;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthMapper {

  private static final Logger LOG = LoggerFilter.getLogger(OAuthMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static String getStrapiAccessToken(AccessTokenData accessTokenData) {
    JsonNode payload = accessTokenData.getPayload();
    if (accessTokenData.getType().equals(OAuthType.GOOGLE)) {
      JsonNode strapi_token = getStrapiToken(payload);
      if (ObjectToolkit.isNullOrEmpty(strapi_token)) return null;
      return strapi_token.get(TokenData.STRAPI_ACCESS_TOKEN.getToken()).textValue();
    }
    return payload.get(TokenData.STRAPI_ACCESS_TOKEN.getToken()).textValue();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static JsonNode getStrapiToken(JsonNode payload) {
    JsonNode strapi_token = null;
    String tokenValue =
        (ObjectToolkit.isNullOrEmpty(payload)
                || ObjectToolkit.isNullOrEmpty(payload.get(TokenData.STRAPI_TOKEN.getToken())))
            ? null
            : payload.get(TokenData.STRAPI_TOKEN.getToken()).asText();
    if (!ObjectUtils.isEmpty(tokenValue)) {
      String jsonString =
          "{\"" + TokenData.STRAPI_ACCESS_TOKEN.getToken() + "\":\"" + tokenValue + "\"}";
      return Mapper.readTree(jsonString);
    }
    return null;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static JsonNode getStrapiTokenFromUser(User user, GoogleModel googleModel) {
    JsonNode strapi_token;
    String tokenValue =
        ObjectToolkit.isNullOrEmpty(user.getAttributes())
                || ObjectToolkit.isNullOrEmpty(
                    user.getAttributes().get(TokenData.STRAPI_USER_TOKEN.getToken()))
            ? null
            : user.getAttributes().get(TokenData.STRAPI_USER_TOKEN.getToken()).toString();
    if (!ObjectToolkit.isNullOrEmpty(tokenValue)) {
      String jsonString =
          "{\"" + TokenData.STRAPI_ACCESS_TOKEN.getToken() + "\":\"" + tokenValue + "\"}";
      strapi_token = Mapper.readTree(jsonString);
      googleModel.getTokenResponse().setStrapiToken(tokenValue);
      return strapi_token;
    }
    return null;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static Map<String, Object> extractStrapiToken(AccessTokenData accessTokenData) {
    if (accessTokenData.getPayload().has(TokenData.STRAPI_TOKEN.getToken())) {
      Map<String, Object> strapiToken = new HashMap<>();
      strapiToken.put(
          TokenData.STRAPI_ACCESS_TOKEN.getToken(),
          accessTokenData.getPayload().get(TokenData.STRAPI_TOKEN.getToken()).asText());
      strapiToken.put("token_type", "Bearer");
      return strapiToken;
    }
    return null;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static OAuthType getOAuthType(
      ClientCredential clientCredential,
      AccessTokenData accessTokenData,
      String grant_type,
      String code) {
    if (!ObjectUtils.isEmpty(grant_type) || !ObjectUtils.isEmpty(code))
      return clientCredential.getAuthType().equals(OAuthType.ALL_TYPE) && !ObjectUtils.isEmpty(code)
          ? OAuthType.GOOGLE
          : (clientCredential.getAuthType().equals(OAuthType.ALL_TYPE)
                  && grant_type.equalsIgnoreCase(GrantType.PASSWORD.type())
              ? OAuthType.BEARER
              : clientCredential.getAuthType());
    else return accessTokenData.getType();
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static URI getFinalRedirectURI(
      ClientCredential clientCredential, RedirectUris redirectUris, String defaultRedirectUri) {
    if (!ObjectToolkit.isNullOrEmpty(clientCredential.getRedirect_uri())
        && clientCredential.getRedirect_uri().containsKey(redirectUris.url()))
      return URI.create(clientCredential.getRedirect_uri().get(redirectUris.url()));
    else return URI.create(defaultRedirectUri);
  }
}
