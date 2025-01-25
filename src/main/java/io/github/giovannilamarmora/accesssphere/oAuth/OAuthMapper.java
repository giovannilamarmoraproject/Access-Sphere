package io.github.giovannilamarmora.accesssphere.oAuth;

import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.grpc.google.model.GoogleModel;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuthMapper {

  private static final Logger LOG = LoggerFilter.getLogger(OAuthMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static JsonNode getStrapiToken(JsonNode payload) {
    JsonNode strapi_token = null;
    String tokenValue =
        (Utilities.isNullOrEmpty(payload)
                || Utilities.isNullOrEmpty(payload.get(TokenData.STRAPI_TOKEN.getToken())))
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
        Utilities.isNullOrEmpty(user.getAttributes())
                || Utilities.isNullOrEmpty(
                    user.getAttributes().get(TokenData.STRAPI_USER_TOKEN.getToken()))
            ? null
            : user.getAttributes().get(TokenData.STRAPI_USER_TOKEN.getToken()).toString();
    if (!Utilities.isNullOrEmpty(tokenValue)) {
      String jsonString =
          "{\"" + TokenData.STRAPI_ACCESS_TOKEN.getToken() + "\":\"" + tokenValue + "\"}";
      strapi_token = Mapper.readTree(jsonString);
      googleModel.getTokenResponse().setStrapiToken(tokenValue);
      return strapi_token;
    }
    return null;
  }
}
