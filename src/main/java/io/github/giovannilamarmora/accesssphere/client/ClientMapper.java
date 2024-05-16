package io.github.giovannilamarmora.accesssphere.client;

import io.github.giovannilamarmora.accesssphere.api.strapi.dto.OAuthStrapiClient;
import io.github.giovannilamarmora.accesssphere.api.strapi.dto.StrapiResponse;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import java.util.List;

import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ClientMapper {

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential fromClientCredentialEntityToClientCredential(
      ClientCredentialEntity clientCredentialEntity) {
    return new ClientCredential(
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
        clientCredentialEntity.getRegistrationToken());
  }
}
