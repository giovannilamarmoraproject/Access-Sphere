package io.github.giovannilamarmora.accesssphere.token.mfa;

import io.github.giovannilamarmora.accesssphere.data.user.dto.User;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.LoginStatus;
import io.github.giovannilamarmora.accesssphere.token.mfa.dto.MFATokenData;
import io.github.giovannilamarmora.accesssphere.token.mfa.entity.MFATokenDataEntity;
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
public class MFATokenDataService {

  private static final Logger LOG = LoggerFilter.getLogger(MFATokenDataService.class);
  @Autowired private IMFATokenDataDAO mfaTokenDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFATokenData save(
      User user, List<String> mfaMethods, String client_id, String session_id, Object payload) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 600000);
    String tempToken = UUID.randomUUID().toString();
    MFATokenDataEntity mfaTokenToSave =
        new MFATokenDataEntity(
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
    MFATokenDataEntity mfaTokenDataEntity = mfaTokenDAO.save(mfaTokenToSave);
    mfaTokenDAO.revokeTokensExcept(
        TokenStatus.REVOKED, tempToken, user.getIdentifier(), System.currentTimeMillis());
    return MFATokenDataMapper.fromMFATokenEntityToMFAToken(mfaTokenDataEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFATokenData completeLogin(
      MFATokenData mfaTokenData, String deviceToken, String mfaMethod) {
    Date now = new Date(System.currentTimeMillis());
    Date exp = new Date(now.toInstant().toEpochMilli() + 2592000000L);
    MFATokenDataEntity mfaTokenToSave =
        new MFATokenDataEntity(
            mfaTokenData.getId(),
            null,
            deviceToken,
            LoginStatus.COMPLETED,
            mfaMethod,
            mfaTokenData.getSubject(),
            mfaTokenData.getClientId(),
            mfaTokenData.getSessionId(),
            mfaTokenData.getIdentifier(),
            now.toInstant().toEpochMilli(),
            exp.toInstant().toEpochMilli(),
            Mapper.writeObjectToString(mfaTokenData.getPayload()),
            TokenStatus.ISSUED);
    MFATokenDataEntity mfaTokenDataEntity = mfaTokenDAO.save(mfaTokenToSave);
    return MFATokenDataMapper.fromMFATokenEntityToMFAToken(mfaTokenDataEntity);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFATokenData getByTempToken(String token) {
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

    List<MFATokenDataEntity> mfaTokenEntities =
        mfaTokenDAO.findByTempTokenOrDeviceToken(extractedToken);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("MFA token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    MFATokenDataEntity mfaToken = mfaTokenEntities.getFirst();

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

    return MFATokenDataMapper.fromMFATokenEntityToMFAToken(mfaToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public MFATokenData getByDeviceToken(String token) {
    if (ObjectToolkit.isNullOrEmpty(token)) {
      LOG.error("❌ Device Token not found, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    List<MFATokenDataEntity> mfaTokenEntities = mfaTokenDAO.findByTempTokenOrDeviceToken(token);

    if (ObjectToolkit.isNullOrEmpty(mfaTokenEntities)
        || ObjectToolkit.isNullOrEmpty(mfaTokenEntities.getFirst())) {
      LOG.error("❌ MFA device token data not found on Database");
      return null;
    }

    MFATokenDataEntity mfaToken = mfaTokenEntities.getFirst();

    return MFATokenDataMapper.fromMFATokenEntityToMFAToken(mfaToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteMFATokenExpired() {
    mfaTokenDAO.deleteExpiredToken();
  }
}
