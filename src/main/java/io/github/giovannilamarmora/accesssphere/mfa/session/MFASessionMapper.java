package io.github.giovannilamarmora.accesssphere.mfa.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.dto.MFAType;
import io.github.giovannilamarmora.accesssphere.mfa.session.dto.MFASession;
import io.github.giovannilamarmora.accesssphere.mfa.session.entity.MFASessionEntity;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
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
public class MFASessionMapper {
  private static final Logger LOG = LoggerFilter.getLogger(MFASessionMapper.class);

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASession toDto(MFASessionEntity mfaSessionEntity) {
    MFASession mfaSession = new MFASession();
    BeanUtils.copyProperties(mfaSessionEntity, mfaSession);
    List<MFAType> mfaMethods =
        Arrays.stream(mfaSessionEntity.getMfaMethods().split(" ")).map(MFAType::valueOf).toList();
    mfaSession.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaSessionEntity.getPayload()))
        mfaSession.setPayload(
            Utils.mapper().readValue(mfaSessionEntity.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaSession;
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static MFASessionEntity toEntity(MFASession mfaSession) {
    MFASessionEntity mfaSessionEntity = new MFASessionEntity();
    BeanUtils.copyProperties(mfaSession, mfaSessionEntity);
    // List<MFAType> mfaMethods = mfaToken.getMfaMethods();
    // mfaTokenEntity.setMfaMethods(mfaMethods);
    try {
      if (!ObjectUtils.isEmpty(mfaSession.getPayload()))
        mfaSessionEntity.setPayload(
            Utils.mapper().readValue(mfaSession.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to JsonNode, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return mfaSessionEntity;
  }
}
