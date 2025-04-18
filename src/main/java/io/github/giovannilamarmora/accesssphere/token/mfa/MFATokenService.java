package io.github.giovannilamarmora.accesssphere.token.mfa;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.LoginStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFAToken;
import io.github.giovannilamarmora.accesssphere.token.mfa.entity.MFATokenEntity;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Logged
public class MFATokenService {

  private static final Logger LOG = LoggerFilter.getLogger(MFATokenService.class);
  @Autowired private IMFATokenDAO mfaTokenDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFAToken save(
      User user, List<String> mfaMethods, String client_id, String session_id, Object payload) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 600000);
    String tempToken = UUID.randomUUID().toString();
    MFATokenEntity mfaTokenToSave =
        new MFATokenEntity(
            null,
            tempToken,
            null,
            LoginStatus.PENDING,
            String.join(" ", mfaMethods),
            user.getUsername(),
            client_id,
            session_id,
            user.getIdentifier(),
            now.toInstant().toEpochMilli(),
            exp.toInstant().toEpochMilli(),
            Mapper.writeObjectToString(payload),
            TokenStatus.ISSUED);
    MFATokenEntity mfaTokenEntity = mfaTokenDAO.save(mfaTokenToSave);
    mfaTokenDAO.revokeTokensExcept(
        TokenStatus.REVOKED, tempToken, user.getIdentifier(), System.currentTimeMillis());
    return MFATokenMapper.fromMFATokenEntityToMFAToken(mfaTokenEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFAToken completeLogin(MFAToken mfaToken, String deviceToken, String mfaMethod) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 2592000000L);
    MFATokenEntity mfaTokenToSave =
        new MFATokenEntity(
            mfaToken.getId(),
            null,
            deviceToken,
            LoginStatus.COMPLETED,
            mfaMethod,
            mfaToken.getSubject(),
            mfaToken.getClientId(),
            mfaToken.getSessionId(),
            mfaToken.getIdentifier(),
            now.toInstant().toEpochMilli(),
            exp.toInstant().toEpochMilli(),
            Mapper.writeObjectToString(mfaToken.getPayload()),
            TokenStatus.ISSUED);
    MFATokenEntity mfaTokenEntity = mfaTokenDAO.save(mfaTokenToSave);
    return MFATokenMapper.fromMFATokenEntityToMFAToken(mfaTokenEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFAToken getByTempTokenOrDeviceToken(String token) {
    if (ObjectToolkit.isNullOrEmpty(token)) {
      LOG.error("Token not found, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    String extractedToken =
        token.startsWith("Bearer ") ? token.replaceFirst("Bearer ", "").trim() : token;

    if (extractedToken.isEmpty()) {
      LOG.error("The current Bearer is empty, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    List<MFATokenEntity> mfaTokenEntities =
        mfaTokenDAO.findByTempTokenOrDeviceToken(extractedToken);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("MFA token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    MFATokenEntity mfaToken = mfaTokenEntities.getFirst();

    Date now = new Date();
    if (now.after(new Date(mfaToken.getExpireDate()))
        || mfaToken.getStatus().equals(TokenStatus.EXPIRED)
        || mfaToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current MFA token is expired on {} with status {}",
          new Date(mfaToken.getExpireDate()),
          mfaToken.getStatus());
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return MFATokenMapper.fromMFATokenEntityToMFAToken(mfaToken);
  }
}
