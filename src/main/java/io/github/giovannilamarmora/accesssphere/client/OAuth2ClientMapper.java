package io.github.giovannilamarmora.accesssphere.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Joiner;
import io.github.giovannilamarmora.accesssphere.client.entity.OAuth2ClientEntity;
import io.github.giovannilamarmora.accesssphere.client.model.OAuth2Client;
import io.github.giovannilamarmora.accesssphere.client.model.RedirectUris;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.dto.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
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
public class OAuth2ClientMapper {

  private static final ObjectMapper mapper = MapperUtils.mapper().build();

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static OAuth2Client toDTO(OAuth2ClientEntity oAuth2ClientEntity) {
    try {
      OAuth2Client oAuth2Client =
          new OAuth2Client(
              oAuth2ClientEntity.getClientId(),
              oAuth2ClientEntity.getExternalClientId(),
              oAuth2ClientEntity.getClientSecret(),
              ObjectUtils.isEmpty(oAuth2ClientEntity.getScopes())
                  ? null
                  : List.of(oAuth2ClientEntity.getScopes().split(" ")),
              ObjectUtils.isEmpty(oAuth2ClientEntity.getRedirect_uri())
                  ? null
                  : parseRedirectUri(oAuth2ClientEntity.getRedirect_uri()),
              oAuth2ClientEntity.getAccessType(),
              oAuth2ClientEntity.getAuthType(),
              oAuth2ClientEntity.getTokenType(),
              oAuth2ClientEntity.getJwtSecret(),
              oAuth2ClientEntity.getJwtExpiration(),
              oAuth2ClientEntity.getJweSecret(),
              oAuth2ClientEntity.getJweExpiration(),
              oAuth2ClientEntity.getRegistrationToken(),
              null, // appRoles popolati da ROLE_DEFINITIONS a livello di ClientService
              oAuth2ClientEntity.getIdToken(),
              oAuth2ClientEntity.getAccessToken(),
              oAuth2ClientEntity.getAuthorize_redirect_status(),
              oAuth2ClientEntity.getMfaEnabled(),
              oAuth2ClientEntity.getEnableRegistration(),
              ObjectUtils.isEmpty(oAuth2ClientEntity.getWebhooks())
                  ? null
                  : mapper.readValue(oAuth2ClientEntity.getWebhooks(), new TypeReference<>() {}));
      oAuth2Client.setId(oAuth2ClientEntity.getId());
      return oAuth2Client;
    } catch (JsonProcessingException e) {
      throw new OAuthException(ExceptionMap.ERR_OAUTH_500, ExceptionMap.ERR_OAUTH_500.getMessage());
    }
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static OAuth2ClientEntity toEntity(OAuth2Client oAuth2Client) {
    return new OAuth2ClientEntity(
        oAuth2Client.getId(),
        oAuth2Client.getClientId(),
        oAuth2Client.getExternalClientId(),
        oAuth2Client.getClientSecret(),
        ObjectUtils.isEmpty(oAuth2Client.getScopes())
            ? null
            : Joiner.on(" ").join(oAuth2Client.getScopes()),
        ObjectUtils.isEmpty(oAuth2Client.getRedirect_uri())
            ? null
            : Mapper.writeObjectToString(oAuth2Client.getRedirect_uri()),
        oAuth2Client.getAccessType(),
        oAuth2Client.getAuthType(),
        oAuth2Client.getTokenType(),
        oAuth2Client.getJwtSecret(),
        oAuth2Client.getJwtExpiration(),
        oAuth2Client.getJweSecret(),
        oAuth2Client.getJweExpiration(),
        oAuth2Client.getRegistrationToken(),
        oAuth2Client.getIdToken(),
        oAuth2Client.getAccessToken(),
        oAuth2Client.getAuthorize_redirect_status(),
        oAuth2Client.getMfaEnabled(),
        oAuth2Client.getEnableRegistration(),
        ObjectUtils.isEmpty(oAuth2Client.getWebhooks())
            ? null
            : Mapper.writeObjectToString(oAuth2Client.getWebhooks()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static void updateEntity(OAuth2ClientEntity existingClient, OAuth2Client oAuth2Client) {
    // Aggiorna i campi dell'entità esistente con i valori del ClientCredential
    existingClient.setExternalClientId(oAuth2Client.getExternalClientId());
    existingClient.setClientSecret(oAuth2Client.getClientSecret());
    existingClient.setScopes(
        ObjectUtils.isEmpty(oAuth2Client.getScopes())
            ? null
            : Joiner.on(" ").join(oAuth2Client.getScopes()));
    existingClient.setRedirect_uri(
        ObjectUtils.isEmpty(oAuth2Client.getRedirect_uri())
            ? null
            : Mapper.writeObjectToString(oAuth2Client.getRedirect_uri()));
    existingClient.setAccessType(oAuth2Client.getAccessType());
    existingClient.setAuthType(oAuth2Client.getAuthType());
    existingClient.setTokenType(oAuth2Client.getTokenType());
    existingClient.setJwtSecret(oAuth2Client.getJwtSecret());
    existingClient.setJwtExpiration(oAuth2Client.getJwtExpiration());
    existingClient.setJweSecret(oAuth2Client.getJweSecret());
    existingClient.setJweExpiration(oAuth2Client.getJweExpiration());
    existingClient.setRegistrationToken(oAuth2Client.getRegistrationToken());
    existingClient.setIdToken(oAuth2Client.getIdToken());
    existingClient.setAccessToken(oAuth2Client.getAccessToken());
    existingClient.setAuthorize_redirect_status(oAuth2Client.getAuthorize_redirect_status());
    existingClient.setMfaEnabled(oAuth2Client.getMfaEnabled());
    existingClient.setEnableRegistration(oAuth2Client.getEnableRegistration());
    existingClient.setWebhooks(
        ObjectUtils.isEmpty(oAuth2Client.getWebhooks())
            ? null
            : Mapper.writeObjectToString(oAuth2Client.getWebhooks()));
  }

  @LogInterceptor(type = LogTimeTracker.ActionType.MAPPER)
  public static List<OAuth2Client> toDTOList(List<OAuth2ClientEntity> clientCredentialEntities) {
    return clientCredentialEntities.stream().map(OAuth2ClientMapper::toDTO).toList();
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
  public static List<RoleDefinition> parseAppRoles(String raw) {
    if (ObjectUtils.isEmpty(raw)) {
      return null;
    }
    String trimmed = raw.trim();
    if (trimmed.startsWith("[")) {
      try {
        List<RoleDefinition> roles = mapper.readValue(trimmed, new TypeReference<List<RoleDefinition>>() {});
        return normalizeAppRoles(roles);
      } catch (Exception ignored) {
        // Fallback in caso di parse JSON non conforme
      }
    }
    String[] tokens = trimmed.split("[,\\s]+");
    List<RoleDefinition> fallbackList = new ArrayList<>();
    for (String token : tokens) {
      if (!token.isBlank()) {
        fallbackList.add(
            new RoleDefinition(token.trim().toUpperCase(), AppRoleType.USER.getValue(), null));
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
  public static List<RoleDefinition> normalizeAppRoles(List<RoleDefinition> roles) {
    if (ObjectUtils.isEmpty(roles)) {
      return null;
    }
    List<RoleDefinition> normalized = new ArrayList<>();
    for (RoleDefinition r : roles) {
      if (r != null && !ObjectUtils.isEmpty(r.getRole())) {
        String roleName = r.getRole().trim().toUpperCase();
        String rawType = r.getType();
        String type =
            (!ObjectUtils.isEmpty(rawType) && !rawType.isBlank())
                ? ("DEFAULT".equalsIgnoreCase(rawType.trim())
                    ? AppRoleType.USER.getValue()
                    : rawType.trim().toUpperCase())
                : AppRoleType.USER.getValue();
        normalized.add(
            new RoleDefinition(r.getId(), roleName, type, r.getDescription(), r.getPermissions()));
      }
    }
    return normalized.isEmpty() ? null : normalized;
  }
}
