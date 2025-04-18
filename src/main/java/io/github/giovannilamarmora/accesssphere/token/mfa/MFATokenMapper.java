package io.github.giovannilamarmora.accesssphere.token.mfa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFAToken;
import io.github.giovannilamarmora.accesssphere.token.mfa.entity.MFATokenEntity;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
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
public class MFATokenMapper {
  private static final Logger LOG = LoggerFilter.getLogger(MFATokenService.class);

  public static MFAToken fromMFATokenEntityToMFAToken(MFATokenEntity mfaTokenEntity) {
    MFAToken mfaToken = new MFAToken();
    BeanUtils.copyProperties(mfaTokenEntity, mfaToken);
    List<MFAType> mfaMethods =
        Arrays.stream(mfaTokenEntity.getMfaMethods().split(" ")).map(MFAType::valueOf).toList();
    mfaToken.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaTokenEntity.getPayload()))
        mfaToken.setPayload(
            Utils.mapper().readValue(mfaTokenEntity.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaToken;
  }

  public static MFATokenEntity fromMFATokenToMFATokenEntity(MFAToken mfaToken) {
    MFATokenEntity mfaTokenEntity = new MFATokenEntity();
    BeanUtils.copyProperties(mfaToken, mfaTokenEntity);
    // List<MFAType> mfaMethods = mfaToken.getMfaMethods();
    // mfaTokenEntity.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaToken.getPayload()))
        mfaTokenEntity.setPayload(
            Utils.mapper().readValue(mfaToken.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaTokenEntity;
  }
}
