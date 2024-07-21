package io.github.giovannilamarmora.accesssphere.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import io.github.giovannilamarmora.utils.utilities.Utilities;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ClientMapper {

  private static final ObjectMapper mapper = MapperUtils.mapper().build();

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential fromClientCredentialEntityToClientCredential(
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
                  : List.of(clientCredentialEntity.getRedirect_uri().split(" ")),
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
              ObjectUtils.isEmpty(clientCredentialEntity.getDefaultRoles())
                  ? null
                  : mapper.readValue(clientCredentialEntity.getDefaultRoles(), AppRole.class),
              ObjectUtils.isEmpty(clientCredentialEntity.getDefaultRoles())
                  ? null
                  : mapper.readValue(
                      clientCredentialEntity.getDefaultRoles(), new TypeReference<>() {}),
              clientCredentialEntity.getIdToken(),
              clientCredentialEntity.getAccessToken(),
              clientCredentialEntity.getStrapiToken());
      clientCredential.setId(clientCredentialEntity.getId());
      return clientCredential;
    } catch (JsonProcessingException e) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_500, ExceptionMap.ERR_OAUTH_500.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredentialEntity fromClientCredentialToClientCredentialEntity(
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
            : Joiner.on(" ").join(clientCredential.getRedirect_uri()),
        clientCredential.getAccessType(),
        clientCredential.getAuthType(),
        clientCredential.getTokenType(),
        clientCredential.getJwtSecret(),
        clientCredential.getJwtExpiration(),
        clientCredential.getJweSecret(),
        clientCredential.getJweExpiration(),
        clientCredential.getRegistrationToken(),
        ObjectUtils.isEmpty(clientCredential.getDefaultRole())
            ? null
            : Utilities.convertObjectToJson(clientCredential.getDefaultRole()),
        ObjectUtils.isEmpty(clientCredential.getAppRoles())
            ? null
            : Utilities.convertObjectToJson(clientCredential.getAppRoles()),
        clientCredential.getIdToken(),
        clientCredential.getAccessToken(),
        clientCredential.getStrapiToken());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static void updateEntityFields(
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
            : Joiner.on(" ").join(clientCredential.getRedirect_uri()));
    existingClient.setAccessType(clientCredential.getAccessType());
    existingClient.setAuthType(clientCredential.getAuthType());
    existingClient.setTokenType(clientCredential.getTokenType());
    existingClient.setJwtSecret(clientCredential.getJwtSecret());
    existingClient.setJwtExpiration(clientCredential.getJwtExpiration());
    existingClient.setJweSecret(clientCredential.getJweSecret());
    existingClient.setJweExpiration(clientCredential.getJweExpiration());
    existingClient.setRegistrationToken(clientCredential.getRegistrationToken());
    existingClient.setDefaultRoles(
        ObjectUtils.isEmpty(clientCredential.getDefaultRole())
            ? null
            : Utilities.convertObjectToJson(clientCredential.getDefaultRole()));
    existingClient.setAppRoles(
        ObjectUtils.isEmpty(clientCredential.getAppRoles())
            ? null
            : Utilities.convertObjectToJson(clientCredential.getAppRoles()));
    existingClient.setIdToken(clientCredential.getIdToken());
    existingClient.setAccessToken(clientCredential.getAccessToken());
    existingClient.setStrapiToken(clientCredential.getStrapiToken());
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<ClientCredential> fromClientCredentialEntitiesToClientCredential(
      List<ClientCredentialEntity> clientCredentialEntities) {
    return clientCredentialEntities.stream()
        .map(ClientMapper::fromClientCredentialEntityToClientCredential)
        .toList();
  }
}
