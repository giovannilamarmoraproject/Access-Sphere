package io.github.giovannilamarmora.accesssphere.token.mfa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFATokenData;
import io.github.giovannilamarmora.accesssphere.token.mfa.entity.MFATokenDataEntity;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
@Logged
public class MFATokenDataMapper {
  private static final Logger LOG = LoggerFilter.getLogger(MFATokenDataService.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFATokenData fromMFATokenEntityToMFAToken(MFATokenDataEntity mfaTokenDataEntity) {
    MFATokenData mfaTokenData = new MFATokenData();
    BeanUtils.copyProperties(mfaTokenDataEntity, mfaTokenData);
    List<MFAType> mfaMethods =
        Arrays.stream(mfaTokenDataEntity.getMfaMethods().split(" ")).map(MFAType::valueOf).toList();
    mfaTokenData.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaTokenDataEntity.getPayload()))
        mfaTokenData.setPayload(
            Utils.mapper().readValue(mfaTokenDataEntity.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaTokenData;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFATokenDataEntity fromMFATokenToMFATokenEntity(MFATokenData mfaTokenData) {
    MFATokenDataEntity mfaTokenDataEntity = new MFATokenDataEntity();
    BeanUtils.copyProperties(mfaTokenData, mfaTokenDataEntity);
    // List<MFAType> mfaMethods = mfaToken.getMfaMethods();
    // mfaTokenEntity.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaTokenData.getPayload()))
        mfaTokenDataEntity.setPayload(
            Utils.mapper().readValue(mfaTokenData.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaTokenDataEntity;
  }
}
