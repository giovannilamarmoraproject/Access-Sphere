package io.github.giovannilamarmora.accesssphere.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ClientMapper {

  private static final ObjectMapper mapper = MapperUtils.mapper().build();

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential toDTO(
      ClientCredentialEntity clientCredentialEntity) {
    try {
      ClientCredential clientCredential =
          new ClientCredential(
              clientCredentialEntity.getClientId(),
              clientCredentialEntity.getExternalClientId(),
              clientCredentialEntity.getClientSecret(),
              ObjectUtils.isEmpty(clientCredentialEntity.getScopes())
                  ? null
                  : List.of(clientCredentialEntity.getScopes().split(" ")),
              ObjectUtils.isEmpty(clientCredentialEntity.getRedirect_uri())
                  ? null
                  : parseRedirectUri(clientCredentialEntity.getRedirect_uri()),
              clientCredentialEntity.getAccessType(),
              clientCredentialEntity.getAuthType(),
              clientCredentialEntity.getTokenType(),
              clientCredentialEntity.getJwtSecret(),
              clientCredentialEntity.getJwtExpiration(),
              clientCredentialEntity.getJweSecret(),
              clientCredentialEntity.getJweExpiration(),
              clientCredentialEntity.getRegistrationToken(),
              /*ObjectUtils.isEmpty(clientCredentialEntity.getDefaultRoles())
              ? null
              : Arrays.stream(clientCredentialEntity.getDefaultRoles().split(" ")).toList()*/
              ObjectUtils.isEmpty(clientCredentialEntity.getAppRoles())
                  ? null
                  : mapper.readValue(
                      clientCredentialEntity.getAppRoles(), new TypeReference<>() {}),
              clientCredentialEntity.getIdToken(),
              clientCredentialEntity.getAccessToken(),
              clientCredentialEntity.getAuthorize_redirect_status(),
              clientCredentialEntity.getMfaEnabled(),
              ObjectUtils.isEmpty(clientCredentialEntity.getWebhooks())
                  ? null
                  : mapper.readValue(
                      clientCredentialEntity.getWebhooks(), new TypeReference<>() {}));
      clientCredential.setId(clientCredentialEntity.getId());
      return clientCredential;
    } catch (JsonProcessingException e) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_500, ExceptionMap.ERR_OAUTH_500.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredentialEntity toEntity(
      ClientCredential clientCredential) {
    return new ClientCredentialEntity(
        clientCredential.getId(),
        clientCredential.getClientId(),
        clientCredential.getExternalClientId(),
        clientCredential.getClientSecret(),
        ObjectUtils.isEmpty(clientCredential.getScopes())
            ? null
            : Joiner.on(" ").join(clientCredential.getScopes()),
        ObjectUtils.isEmpty(clientCredential.getRedirect_uri())
            ? null
            : Mapper.writeObjectToString(clientCredential.getRedirect_uri()),
        clientCredential.getAccessType(),
        clientCredential.getAuthType(),
        clientCredential.getTokenType(),
        clientCredential.getJwtSecret(),
        clientCredential.getJwtExpiration(),
        clientCredential.getJweSecret(),
        clientCredential.getJweExpiration(),
        clientCredential.getRegistrationToken(),
        ObjectUtils.isEmpty(clientCredential.getAppRoles())
            ? null
            : Mapper.writeObjectToString(clientCredential.getAppRoles()),
        clientCredential.getIdToken(),
        clientCredential.getAccessToken(),
        clientCredential.getAuthorize_redirect_status(),
        clientCredential.getMfaEnabled(),
        ObjectUtils.isEmpty(clientCredential.getWebhooks())
            ? null
            : Mapper.writeObjectToString(clientCredential.getWebhooks()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static void updateEntity(
      ClientCredentialEntity existingClient, ClientCredential clientCredential) {
    // Aggiorna i campi dell'entità esistente con i valori del ClientCredential
    existingClient.setExternalClientId(clientCredential.getExternalClientId());
    existingClient.setClientSecret(clientCredential.getClientSecret());
    existingClient.setScopes(
        ObjectUtils.isEmpty(clientCredential.getScopes())
            ? null
            : Joiner.on(" ").join(clientCredential.getScopes()));
    existingClient.setRedirect_uri(
        ObjectUtils.isEmpty(clientCredential.getRedirect_uri())
            ? null
            : Mapper.writeObjectToString(clientCredential.getRedirect_uri()));
    existingClient.setAccessType(clientCredential.getAccessType());
    existingClient.setAuthType(clientCredential.getAuthType());
    existingClient.setTokenType(clientCredential.getTokenType());
    existingClient.setJwtSecret(clientCredential.getJwtSecret());
    existingClient.setJwtExpiration(clientCredential.getJwtExpiration());
    existingClient.setJweSecret(clientCredential.getJweSecret());
    existingClient.setJweExpiration(clientCredential.getJweExpiration());
    existingClient.setRegistrationToken(clientCredential.getRegistrationToken());
    existingClient.setAppRoles(
        ObjectUtils.isEmpty(clientCredential.getAppRoles())
            ? null
            : Mapper.writeObjectToString(clientCredential.getAppRoles()));
    existingClient.setIdToken(clientCredential.getIdToken());
    existingClient.setAccessToken(clientCredential.getAccessToken());
    existingClient.setAuthorize_redirect_status(clientCredential.getAuthorize_redirect_status());
    existingClient.setMfaEnabled(clientCredential.getMfaEnabled());
    existingClient.setWebhooks(
        ObjectUtils.isEmpty(clientCredential.getWebhooks())
            ? null
            : Mapper.writeObjectToString(clientCredential.getWebhooks()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<ClientCredential> toDTOList(
      List<ClientCredentialEntity> clientCredentialEntities) {
    return clientCredentialEntities.stream()
        .map(ClientMapper::toDTO)
        .toList();
  }

  /**
   * Esegue il parsing resiliente del campo redirect_uri memorizzato a database.
   * Supporta sia il formato JSON (mappa con fallback_uri e redirect_uri),
   * sia eventuali stringhe legacy plain-text per garantire piena retrocompatibilita.
   *
   * @param raw Stringa grezza proveniente dalla colonna REDIRECT_URI a DB
   * @return Mappa contenente i redirect URI configurati, oppure null se la stringa e vuota.
   */
  public static Map<String, String> parseRedirectUri(String raw) {
    if (ObjectUtils.isEmpty(raw)) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("{")) {
      try {
        return Mapper.readObject(trimmed, new TypeReference<Map<String, String>>() {});
      } catch (Exception ignored) {
        // Fallback di sicurezza in caso di JSON non conforme
      }
    }
    Map<String, String> map = new LinkedHashMap<>();
    map.put(RedirectUris.REDIRECT_URI.url(), trimmed.replace(",", " "));
    return map;
  }
}
