package io.github.giovannilamarmora.accesssphere.token.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.data.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.auth.TokenUtils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.Date;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class AccessTokenService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private IAccessTokenDAO accessTokenDAO;
  private static final Long REFRESH_TOKEN_1D = 86400000L;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AccessTokenData save(
      JWTData jwtData,
      String refreshToken,
      String accessToken,
      String idToken,
      String client_id,
      String session_id,
      List<String> roles,
      Object payload) {
    AccessTokenEntity accessTokenToBeSaved = new AccessTokenEntity();
    try {
      accessTokenToBeSaved =
          new AccessTokenEntity(
              null,
              refreshToken,
              accessToken,
              idToken,
              session_id,
              client_id,
              jwtData.getSub(),
              jwtData.getSubject_type(),
              jwtData.getEmail(),
              jwtData.getIdentifier(),
              jwtData.getType(),
              jwtData.getIat(),
              jwtData.getExp() + REFRESH_TOKEN_1D,
              jwtData.getExp(),
              Utils.mapper().writeValueAsString(payload),
              TokenStatus.ISSUED,
              Utils.mapper().writeValueAsString(roles));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error happen during creating and storing refresh token, message is {}",
          e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    AccessTokenEntity accessTokenSaved = accessTokenDAO.save(accessTokenToBeSaved);
    accessTokenDAO.revokeTokensExcept(
        TokenStatus.REVOKED, accessToken, jwtData.getIdentifier(), System.currentTimeMillis());
    if (ObjectUtils.isEmpty(accessTokenSaved)) {
      LOG.error("Refresh Token not saved");
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return AccessTokenMapper.fromAccessTokenEntityToData(accessTokenSaved);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AccessTokenData getByAccessTokenOrIdToken(String token) {
    String extractedToken =
        token.startsWith("Bearer ") ? token.replaceFirst("Bearer ", "").trim() : token;

    if (extractedToken.isEmpty()) {
      LOG.error("The current Bearer is empty, please provide a valid token");
      throw new TokenException(ExceptionMap.ERR_TOKEN_401, ExceptionMap.ERR_TOKEN_401.getMessage());
    }

    String hashedToken = TokenUtils.hashingToken(extractedToken);
    List<AccessTokenEntity> accessTokens = accessTokenDAO.findByTokenHash(hashedToken);

    if (ObjectToolkit.isNullOrEmpty(accessTokens)
        || ObjectToolkit.isNullOrEmpty(accessTokens.getFirst())) {
      LOG.error("Access token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    AccessTokenEntity accessToken = accessTokens.getFirst();

    Date now = new Date();
    if (now.after(new Date(accessToken.getAccessExpireDate()))
        || accessToken.getStatus().equals(TokenStatus.EXPIRED)
        || accessToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current access token is expired on {}", new Date(accessToken.getAccessExpireDate()));
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return AccessTokenMapper.fromAccessTokenEntityToData(accessToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AccessTokenData getByRefreshToken(String token) {
    String hashedToken =
        token.contains("Bearer") ? TokenUtils.hashingToken(token.split("Bearer ")[1]) : token;
    List<AccessTokenEntity> accessTokens = accessTokenDAO.findByTokenHash(hashedToken);

    if (ObjectToolkit.isNullOrEmpty(accessTokens)
        || ObjectToolkit.isNullOrEmpty(accessTokens.getFirst())) {
      LOG.error("Refresh token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    AccessTokenEntity accessToken = accessTokens.getFirst();

    Date now = new Date();
    if (now.after(new Date(accessToken.getRefreshExpireDate()))
        || accessToken.getStatus().equals(TokenStatus.EXPIRED)
        || accessToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current refresh token is expired on {}",
          new Date(accessToken.getRefreshExpireDate()));
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return AccessTokenMapper.fromAccessTokenEntityToData(accessToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void revokeTokenByIdentifier(String identifier) {
    accessTokenDAO.revokeToken(TokenStatus.REVOKED, identifier);
    LOG.info("Token revoked on Database");
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteAccessTokenExpired() {
    accessTokenDAO.deleteExpiredToken();
  }
}
