package io.github.giovannilamarmora.accesssphere.token.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.data.entity.AccessTokenEntity;
import io.github.giovannilamarmora.accesssphere.token.data.model.AccessTokenData;
import io.github.giovannilamarmora.accesssphere.token.dto.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

@Service
public class AccessTokenService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());
  @Autowired private IAccessTokenDAO accessTokenDAO;
  private static final Long REFRESH_TOKEN_1D = 86400000L;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AccessTokenData save(
      JWTData jwtData,
      String refreshToken,
      String accessToken,
      String idToken,
      String client_id,
      Object payload) {
    AccessTokenEntity accessTokenToBeSaved = new AccessTokenEntity();
    try {
      accessTokenToBeSaved =
          new AccessTokenEntity(
              null,
              refreshToken,
              accessToken,
              idToken,
              client_id,
              jwtData.getSub(),
              jwtData.getEmail(),
              jwtData.getIdentifier(),
              jwtData.getType(),
              jwtData.getIat(),
              jwtData.getExp() + REFRESH_TOKEN_1D,
              Utils.mapper().writeValueAsString(payload));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error happen during creating and storing refresh token, message is {}",
          e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    AccessTokenEntity accessTokenSaved = accessTokenDAO.save(accessTokenToBeSaved);
    if (ObjectUtils.isEmpty(accessTokenSaved)) {
      LOG.error("Refresh Token not saved");
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return AccessTokenMapper.fromAccessTokenEntityToData(accessTokenSaved);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public AccessTokenData getByAccessTokenIdTokenOrRefreshToken(String token) {
    String hashedToken =
        token.contains("Bearer") ? Utils.hashingToken(token.split("Bearer ")[1]) : token;
    AccessTokenEntity accessToken = accessTokenDAO.findByTokenHash(hashedToken);

    if (ObjectUtils.isEmpty(accessToken)) {
      LOG.error("Access token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }
    Date now = new Date();
    if (now.after(new Date(accessToken.getExpireDate()))) {
      LOG.error("The current token is expired on {}", new Date(accessToken.getExpireDate()));
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return AccessTokenMapper.fromAccessTokenEntityToData(accessToken);
  }
}
