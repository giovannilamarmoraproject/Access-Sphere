package io.github.giovannilamarmora.accesssphere.oAuth.authorization;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.entity.OAuth2AuthorizationEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
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
public class OAuth2AuthorizationService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  @Autowired private IOAuth2AuthorizationDAO accessTokenDAO;

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public OAuth2Authorization save(
      JWTData jwtData,
      String refreshToken,
      String accessToken,
      String idToken,
      String client_id,
      String session_id,
      List<String> roles,
      List<String> permissions,
      Object payload) {
    OAuth2AuthorizationEntity accessTokenToBeSaved =
        OAuth2AuthorizationMapper.toEntity(
            jwtData,
            refreshToken,
            accessToken,
            idToken,
            client_id,
            session_id,
            roles,
            permissions,
            payload);
    OAuth2AuthorizationEntity accessTokenSaved = accessTokenDAO.save(accessTokenToBeSaved);
    accessTokenDAO.revokeTokensExcept(
        TokenStatus.REVOKED, accessToken, jwtData.getIdentifier(), System.currentTimeMillis());
    if (ObjectUtils.isEmpty(accessTokenSaved)) {
      LOG.error("Refresh Token not saved");
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return OAuth2AuthorizationMapper.toDto(accessTokenSaved);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public OAuth2Authorization getByAccessTokenOrIdToken(String token) {
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

    String hashedToken = TokenUtils.hashingToken(extractedToken);
    List<OAuth2AuthorizationEntity> accessTokens = accessTokenDAO.findByTokenHash(hashedToken);

    if (ObjectToolkit.isNullOrEmpty(accessTokens)
        || ObjectToolkit.isNullOrEmpty(accessTokens.getFirst())) {
      LOG.error("Access token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    OAuth2AuthorizationEntity accessToken = accessTokens.getFirst();

    Date now = new Date();
    if (now.after(new Date(accessToken.getAccessExpireDate()))
        || accessToken.getStatus().equals(TokenStatus.EXPIRED)
        || accessToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current access token is expired on {} with status {}",
          new Date(accessToken.getAccessExpireDate()),
          accessToken.getStatus());
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return OAuth2AuthorizationMapper.toDto(accessToken);
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public OAuth2Authorization getByRefreshToken(String token) {
    String hashedToken =
        token.contains("Bearer") ? TokenUtils.hashingToken(token.split("Bearer ")[1]) : token;
    List<OAuth2AuthorizationEntity> accessTokens = accessTokenDAO.findByTokenHash(hashedToken);

    if (ObjectToolkit.isNullOrEmpty(accessTokens)
        || ObjectToolkit.isNullOrEmpty(accessTokens.getFirst())) {
      LOG.error("Refresh token data not found on Database");
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    OAuth2AuthorizationEntity accessToken = accessTokens.getFirst();

    Date now = new Date();
    if (now.after(new Date(accessToken.getRefreshExpireDate()))
        || accessToken.getStatus().equals(TokenStatus.EXPIRED)
        || accessToken.getStatus().equals(TokenStatus.REVOKED)) {
      LOG.error(
          "The current refresh token is expired on {} with status {}",
          new Date(accessToken.getRefreshExpireDate()),
          accessToken.getStatus());
      throw new TokenException(ExceptionMap.ERR_OAUTH_401, ExceptionMap.ERR_OAUTH_401.getMessage());
    }

    return OAuth2AuthorizationMapper.toDto(accessToken);
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
