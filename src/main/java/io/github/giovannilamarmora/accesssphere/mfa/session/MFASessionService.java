package io.github.giovannilamarmora.accesssphere.mfa.session;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.mfa.session.dto.LoginStatus;
import io.github.giovannilamarmora.accesssphere.mfa.session.dto.MFASession;
import io.github.giovannilamarmora.accesssphere.mfa.session.entity.MFASessionEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
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
public class MFASessionService {

  private static final Logger LOG = LoggerFilter.getLogger(MFASessionService.class);
  @Autowired private IMFASessionDAO mfaTokenDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession save(
      User user, List<String> mfaMethods, String client_id, String session_id, Object payload) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 600000);
    String tempToken = UUID.randomUUID().toString();
    MFASessionEntity mfaTokenToSave =
        new MFASessionEntity(
            null,
            tempToken,
            null,
            LoginStatus.PENDING,
            String.join(" ", mfaMethods.stream().distinct().toList()),
            user.getUsername(),
            client_id,
            session_id,
            user.getIdentifier(),
            now.toInstant().toEpochMilli(),
            exp.toInstant().toEpochMilli(),
            Mapper.writeObjectToString(payload),
            TokenStatus.ISSUED);
    MFASessionEntity mfaSessionEntity = mfaTokenDAO.save(mfaTokenToSave);
    mfaTokenDAO.revokeTokensExcept(
        TokenStatus.REVOKED, tempToken, user.getIdentifier(), System.currentTimeMillis());
    return MFASessionMapper.toDto(mfaSessionEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession completeLogin(MFASession mfaSession, String deviceToken, String mfaMethod) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 2592000000L);
    MFASessionEntity mfaTokenToSave =
        new MFASessionEntity(
            mfaSession.getId(),
            null,
            deviceToken,
            LoginStatus.COMPLETED,
            mfaMethod,
            mfaSession.getSubject(),
            mfaSession.getClientId(),
            mfaSession.getSessionId(),
            mfaSession.getIdentifier(),
            now.toInstant().toEpochMilli(),
            exp.toInstant().toEpochMilli(),
            Mapper.writeObjectToString(mfaSession.getPayload()),
            TokenStatus.ISSUED);
    MFASessionEntity mfaSessionEntity = mfaTokenDAO.save(mfaTokenToSave);
    return MFASessionMapper.toDto(mfaSessionEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession getByTempToken(String token) {
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

    List<MFASessionEntity> mfaTokenEntities =
        mfaTokenDAO.findByTempTokenOrDeviceToken(extractedToken);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("MFA token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    MFASessionEntity mfaToken = mfaTokenEntities.getFirst();

    Date now = new Date();
    if (now.after(new Date(mfaToken.getExpireDate()))
        || !mfaToken.getLoginStatus().equals(LoginStatus.PENDING)
        || mfaToken.getStatus().equals(TokenStatus.EXPIRED)
        || mfaToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current MFA token is expired on {} with status {}",
          new Date(mfaToken.getExpireDate()),
          mfaToken.getStatus());
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return MFASessionMapper.toDto(mfaToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFASession getByDeviceToken(String token) {
    if (ObjectToolkit.isNullOrEmpty(token)) {
      LOG.error("❌ Device Token not found, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    List<MFASessionEntity> mfaTokenEntities = mfaTokenDAO.findByTempTokenOrDeviceToken(token);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("❌ MFA device token data not found on Database");
      return null;
    }

    MFASessionEntity mfaToken = mfaTokenEntities.getFirst();

    return MFASessionMapper.toDto(mfaToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteMFATokenExpired() {
    mfaTokenDAO.deleteExpiredToken();
  }
}
