package io.github.giovannilamarmora.accesssphere.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.entity.ClientCredentialEntity;
import io.github.giovannilamarmora.accesssphere.client.model.ClientCredential;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRole;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.utilities.Mapper;
import io.github.giovannilamarmora.utils.utilities.MapperUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

@Component
public class ClientMapper {

  private static final ObjectMapper mapper = MapperUtils.mapper().build();

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static ClientCredential toDTO(ClientCredentialEntity clientCredentialEntity) {
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
              null, // appRoles popolati da ROLE_DEFINITIONS a livello di ClientService
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
  public static ClientCredentialEntity toEntity(ClientCredential clientCredential) {
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
    return clientCredentialEntities.stream().map(ClientMapper::toDTO).toList();
  }

  /**
   * Esegue il parsing resiliente del campo redirect_uri memorizzato a database. Supporta sia il
   * formato JSON (mappa con fallback_uri e redirect_uri), sia eventuali stringhe legacy plain-text
   * per garantire piena retrocompatibilita.
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

  /**
   * Esegue il parsing resiliente del campo APP_ROLES memorizzato a database o proveniente da
   * payload esterno. Supporta:
   *
   * <ul>
   *   <li>Array JSON di oggetti AppRole completi (es. [{"role":"...", "type":"DEFAULT"}])
   *   <li>Array JSON di stringhe (es. ["ROLE_ADMIN", "ROLE_TECH"])
   *   <li>Stringhe semplici delimitate da spazi o virgole
   * </ul>
   *
   * @param raw Stringa grezza della colonna APP_ROLES.
   * @return Lista di {@link AppRole} normalizzati, oppure null se raw e' vuoto.
   */
  public static List<AppRole> parseAppRoles(String raw) {
    if (ObjectUtils.isEmpty(raw)) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("[")) {
      try {
        List<AppRole> roles = mapper.readValue(trimmed, new TypeReference<List<AppRole>>() {});
        return normalizeAppRoles(roles);
      } catch (Exception ignored) {
        // Fallback in caso di parse JSON non conforme
      }
    }
    String[] tokens = trimmed.split("[,\\s]+");
    List<AppRole> fallbackList = new ArrayList<>();
    for (String token : tokens) {
      if (!token.isBlank()) {
        fallbackList.add(
            new AppRole(token.trim().toUpperCase(), AppRoleType.USER.getValue(), null));
      }
    }
    return fallbackList.isEmpty() ? null : fallbackList;
  }

  /**
   * Normalizza una lista di ruoli assicurando che il nome ruolo sia maiuscolo, che non contenga
   * elementi nulli e che la tipologia sia valorizzata (con fallback su USER).
   *
   * @param roles Lista ruoli da normalizzare.
   * @return Lista di ruoli normalizzati, oppure null se la lista in ingresso e' vuota.
   */
  public static List<AppRole> normalizeAppRoles(List<AppRole> roles) {
    if (ObjectUtils.isEmpty(roles)) {
      return null;
    }
    List<AppRole> normalized = new ArrayList<>();
    for (AppRole r : roles) {
      if (r != null && !ObjectUtils.isEmpty(r.getRole())) {
        String roleName = r.getRole().trim().toUpperCase();
        String rawType = r.getType();
        String type =
            (!ObjectUtils.isEmpty(rawType) && !rawType.isBlank())
                ? ("DEFAULT".equalsIgnoreCase(rawType.trim()) ? AppRoleType.USER.getValue() : rawType.trim().toUpperCase())
                : AppRoleType.USER.getValue();
        normalized.add(new AppRole(r.getId(), roleName, type, r.getDescription(), r.getPermissions()));
      }
    }
    return normalized.isEmpty() ? null : normalized;
  }
}
