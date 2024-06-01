package io.github.giovannilamarmora.accesssphere.data;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.data.user.entity.UserEntity;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import org.slf4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class DataValidator {

  private static final Logger LOG = LoggerFilter.getLogger(DataValidator.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateUser(UserEntity userEntity) {
    if (ObjectUtils.isEmpty(userEntity)) {
      LOG.error("User not found into database");
      throw new OAuthException(ExceptionMap.ERR_OAUTH_404, ExceptionMap.ERR_OAUTH_404.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.VALIDATOR)
  public static void validateStrapiResponse(ResponseEntity<StrapiResponse> strapiResponseRes) {
    if (ObjectUtils.isEmpty(strapiResponseRes.getBody())) {
      LOG.error("Strapi response on user is null");
      throw new OAuthException(
          ExceptionMap.ERR_STRAPI_400, ExceptionMap.ERR_STRAPI_400.getMessage());
    }

    if (ObjectUtils.isEmpty(strapiResponseRes.getBody().getUser())) {
      LOG.error("Strapi response on user is null");
      throw new OAuthException(
          ExceptionMap.ERR_STRAPI_400, ExceptionMap.ERR_STRAPI_400.getMessage());
    }
  }
}
