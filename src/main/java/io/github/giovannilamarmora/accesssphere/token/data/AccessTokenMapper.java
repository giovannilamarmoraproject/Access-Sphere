package io.github.giovannilamarmora.accesssphere.token.data;

import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

@Component
public class AccessTokenMapper {

  private static final Logger LOG = LoggerFactory.getLogger(AccessTokenMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static AccessTokenData fromAccessTokenEntityToData(AccessTokenEntity accessTokenEntity) {
    AccessTokenData accessTokenData = new AccessTokenData();
    BeanUtils.copyProperties(accessTokenEntity, accessTokenData);
    return accessTokenData;
  }
}
