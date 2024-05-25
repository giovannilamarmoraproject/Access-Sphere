package io.github.giovannilamarmora.accesssphere.token.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.utilities.LoggerFilter;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class AccessTokenMapper {

  private static final Logger LOG = LoggerFilter.getLogger(AccessTokenMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static AccessTokenData fromAccessTokenEntityToData(AccessTokenEntity accessTokenEntity) {
    AccessTokenData accessTokenData = new AccessTokenData();
    BeanUtils.copyProperties(accessTokenEntity, accessTokenData);
    try {
      if (!ObjectUtils.isEmpty(accessTokenEntity.getPayload()))
        accessTokenData.setPayload(
            Utils.mapper().readValue(accessTokenEntity.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return accessTokenData;
  }
}
