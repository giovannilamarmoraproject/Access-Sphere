package io.github.giovannilamarmora.accesssphere.oAuth.authorization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.entity.OAuth2AuthorizationEntity;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.OAuth2Authorization;
import io.github.giovannilamarmora.accesssphere.oAuth.authorization.model.TokenStatus;
import io.github.giovannilamarmora.accesssphere.token.TokenException;
import io.github.giovannilamarmora.accesssphere.token.model.JWTData;
import io.github.giovannilamarmora.accesssphere.utilities.Utils;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuth2AuthorizationMapper {

  private static final Long REFRESH_TOKEN_1D = 86400000L;

  private static final Logger LOG = LoggerFilter.getLogger(OAuth2AuthorizationMapper.class);

  public static OAuth2Authorization toDto(OAuth2AuthorizationEntity oAuth2AuthorizationEntity) {
    OAuth2Authorization oAuth2Authorization = new OAuth2Authorization();
    BeanUtils.copyProperties(oAuth2AuthorizationEntity, oAuth2Authorization);
    oAuth2Authorization.setRoles(
        Mapper.readObject(oAuth2AuthorizationEntity.getRoles(), new TypeReference<>() {}));
    oAuth2Authorization.setPermissions(
        Mapper.readObject(oAuth2AuthorizationEntity.getPermissions(), new TypeReference<>() {}));
    try {
      if (!ObjectUtils.isEmpty(oAuth2AuthorizationEntity.getPayload()))
        oAuth2Authorization.setPayload(
            Utils.mapper()
                .readValue(oAuth2AuthorizationEntity.getPayload().toString(), JsonNode.class));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error occurred during parsing payload to string, message is {}", e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return oAuth2Authorization;
  }

  public static OAuth2AuthorizationEntity toEntity(
      JWTData jwtData,
      String refreshToken,
      String accessToken,
      String idToken,
      String client_id,
      String session_id,
      List<String> roles,
      List<String> permissions,
      Object payload) {
    OAuth2AuthorizationEntity accessTokenToBeSaved;
    try {
      accessTokenToBeSaved =
          new OAuth2AuthorizationEntity(
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
              Utils.mapper().writeValueAsString(roles),
              Utils.mapper().writeValueAsString(permissions));
    } catch (JsonProcessingException e) {
      LOG.error(
          "An error happen during creating and storing refresh token, message is {}",
          e.getMessage());
      throw new TokenException(ExceptionMap.ERR_TOKEN_500, ExceptionMap.ERR_TOKEN_500.getMessage());
    }
    return accessTokenToBeSaved;
  }
}
