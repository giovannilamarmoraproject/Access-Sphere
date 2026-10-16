package io.github.giovannilamarmora.accesssphere.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientDataResponse;
import io.github.giovannilamarmora.accesssphere.client.dto.OAuth2ClientRequest;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.roles.mapper.RoleDefinitionMapper;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class OAuth2ClientMapper {

  public static List<OAuth2ClientDataResponse> toResponseList(List<OAuth2Client> oAuth2Client) {
    return oAuth2Client.stream().map(OAuth2ClientMapper::toResponse).toList();
  }

  public static OAuth2ClientDataResponse toResponse(OAuth2Client oAuth2Client) {
    OAuth2ClientDataResponse oAuth2ClientResponse = new OAuth2ClientDataResponse();
    BeanUtils.copyProperties(oAuth2Client, oAuth2ClientResponse);
    oAuth2ClientResponse.setAppRoles(
        RoleDefinitionMapper.toResponseList(oAuth2Client.getAppRoles()));
    return oAuth2ClientResponse;
  }

  public static OAuth2Client toModel(OAuth2ClientRequest oAuth2ClientRequest) {
    OAuth2Client oAuth2Client = new OAuth2Client();
    BeanUtils.copyProperties(oAuth2ClientRequest, oAuth2Client);
    oAuth2Client.setAppRoles(
        RoleDefinitionMapper.requestsToModelList(
            oAuth2ClientRequest.getAppRoles(), oAuth2ClientRequest.getClientId()));
    return oAuth2Client;
  }

  public static List<OAuth2Client> toModelList(List<OAuth2ClientEntity> oAuth2ClientEntities) {
    return oAuth2ClientEntities.stream().map(OAuth2ClientMapper::toModel).toList();
  }

  public static OAuth2Client toModel(OAuth2ClientEntity oAuth2ClientEntity) {
    OAuth2Client oAuth2Client = new OAuth2Client();
    BeanUtils.copyProperties(oAuth2ClientEntity, oAuth2Client);
    if (!ObjectToolkit.isNullOrEmpty(oAuth2ClientEntity.getScopes())) {
      oAuth2Client.setScopes(List.of(oAuth2ClientEntity.getScopes().split(" ")));
    }
    if (!ObjectToolkit.isNullOrEmpty(oAuth2ClientEntity.getRedirect_uri())) {
      oAuth2Client.setRedirect_uri(parseRedirectUri(oAuth2ClientEntity.getRedirect_uri()));
    }
    if (!ObjectToolkit.isNullOrEmpty(oAuth2ClientEntity.getWebhooks())) {
      oAuth2Client.setWebhooks(
          Mapper.readObject(oAuth2ClientEntity.getWebhooks(), new TypeReference<>() {}));
    }
    return oAuth2Client;
  }

  public static OAuth2ClientEntity toEntity(OAuth2Client oAuth2Client) {
    OAuth2ClientEntity oAuth2ClientEntity = new OAuth2ClientEntity();
    BeanUtils.copyProperties(oAuth2Client, oAuth2ClientEntity);
    if (!ObjectToolkit.isNullOrEmpty(oAuth2Client.getScopes())) {
      oAuth2ClientEntity.setScopes(Joiner.on(" ").join(oAuth2Client.getScopes()));
    }
    if (!ObjectToolkit.isNullOrEmpty(oAuth2Client.getRedirect_uri())) {
      oAuth2ClientEntity.setRedirect_uri(
          Mapper.writeObjectToString(oAuth2Client.getRedirect_uri()));
    }
    if (!ObjectToolkit.isNullOrEmpty(oAuth2Client.getWebhooks())) {
      oAuth2ClientEntity.setWebhooks(Mapper.writeObjectToString(oAuth2Client.getWebhooks()));
    }
    return oAuth2ClientEntity;
  }

  /**
   * Esegue il parsing resiliente del campo redirect_uri memorizzato a database. Supporta sia il
   * formato JSON (mappa con fallback_uri e redirect_uri), sia eventuali stringhe legacy plain-text
   * per garantire piena retro compatibilita.
   *
   * @param raw Stringa grezza proveniente dalla colonna REDIRECT_URI a DB
   * @return Mappa contenente i redirect URI configurati, oppure null se la stringa e vuota.
   */
  private static Map<String, String> parseRedirectUri(String raw) {
    if (ObjectUtils.isEmpty(raw)) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("{")) {
      try {
        return Mapper.readObject(trimmed, new TypeReference<>() {});
      } catch (Exception ignored) {
        // Fallback di sicurezza in caso di JSON non conforme
      }
    }
    Map<String, String> map = new LinkedHashMap<>();
    map.put(RedirectUris.REDIRECT_URI.url(), trimmed.replace(",", " "));
    return map;
  }
}
