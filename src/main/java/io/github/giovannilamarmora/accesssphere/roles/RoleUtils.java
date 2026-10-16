package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.AppRoleType;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Utility di dominio condivisa per le operazioni sui ruoli (normalizzazione chiavi, indicizzazione
 * in memoria, scoping multi-tenant, validazione contro catalogo ed arricchimento DTO).
 */
public final class RoleUtils {

  private static final Logger LOG = LoggerFilter.getLogger(RoleUtils.class);
  private static final String ROLE_UTILS = "Role Utils";

  private RoleUtils() {}

  /**
   * Normalizza una stringa (es. clientId o nome ruolo) rimuovendo spazi e convertendo in maiuscolo.
   *
   * @param value Stringa da normalizzare.
   * @return Stringa in maiuscolo o null se vuota.
   */
  public static String normalizeUpperCase(String value) {
    return ObjectToolkit.isNullOrEmpty(value) ? null : value.trim().toUpperCase();
  }

  /**
   * Normalizza la tipologia applicativa del ruolo tramite {@link AppRoleType#fromString(String)}.
   *
   * @param rawType Tipologia grezza.
   * @return Valore canonico del tipo ruolo (default {@code USER}).
   */
  public static String normalizeRoleType(String rawType) {
    return AppRoleType.fromString(rawType).getValue();
  }

  /**
   * Costruisce e normalizza la chiave composita CLIENT_ID:ROLE (in maiuscolo e senza spazi).
   *
   * @param clientId Identificatore univoco del client OAuth2.
   * @param role Nome del ruolo applicativo (es. ROLE_ADMIN).
   * @return Chiave composita normalizzata "CLIENT_ID:ROLE".
   */
  public static String buildRoleKey(String clientId, String role) {
    return (clientId != null ? clientId.trim().toUpperCase() : "")
        + ":"
        + (role != null ? role.trim().toUpperCase() : "");
  }

  /**
   * Estrae il set dei Client ID autorizzati (normalizzati in maiuscolo) dalla lista di definizioni
   * di ruolo assegnate all'operatore.
   *
   * @param roles Lista di definizioni dei ruoli autorizzati per l'operatore.
   * @return Set di Client ID in maiuscolo.
   */
  public static Set<String> extractAuthorizedClientIds(List<RoleDefinition> roles) {
    if (ObjectToolkit.isNullOrEmpty(roles)) return Set.of();
    return roles.stream()
        .map(RoleDefinition::getClientId)
        .filter(clientId -> !ObjectToolkit.isNullOrEmpty(clientId))
        .map(clientId -> clientId.trim().toUpperCase())
        .collect(Collectors.toSet());
  }

  /**
   * Verifica se un Client ID rientra nel perimetro autorizzato dell'operatore.
   *
   * @param clientId Client ID da verificare.
   * @param authorizedRoles Ruoli autorizzati dell'operatore.
   * @param isAdmin Flag che indica se l'operatore ha privilegi globali di Super Admin.
   * @return true se l'accesso al client e consentito, false altrimenti.
   */
  public static boolean isClientAuthorized(
      String clientId, List<RoleDefinition> authorizedRoles, boolean isAdmin) {
    if (isAdmin) return true;
    if (ObjectToolkit.isNullOrEmpty(clientId)) return false;
    return extractAuthorizedClientIds(authorizedRoles).contains(clientId.trim().toUpperCase());
  }

  /**
   * Preserva i ruoli utente appartenenti a client esterni al perimetro di un operatore delegato,
   * combinandoli con le modifiche richieste sui client autorizzati.
   *
   * @param requestedRoles Lista di ruoli inviata nella richiesta di aggiornamento.
   * @param existingRoles Lista di ruoli attualmente assegnati all'utente.
   * @param authorizedRoles Ruoli autorizzati dell'operatore chiamante.
   * @param isAdmin Flag che indica se l'operatore e Super Admin.
   * @return Lista consolidata di ruoli da persistere.
   */
  public static List<UserClientRole> mergeScopedUserRoles(
      List<UserClientRole> requestedRoles,
      List<UserClientRole> existingRoles,
      List<RoleDefinition> authorizedRoles,
      boolean isAdmin) {
    List<UserClientRole> safeRequested =
        requestedRoles != null ? requestedRoles : Collections.emptyList();
    if (isAdmin || ObjectToolkit.isNullOrEmpty(existingRoles)) {
      return safeRequested;
    }

    Set<String> authorizedClientIds = extractAuthorizedClientIds(authorizedRoles);
    List<UserClientRole> merged = new ArrayList<>();

    // Preserva i ruoli preesistenti su client fuori dal perimetro dell'operatore delegato
    existingRoles.stream()
        .filter(
            r ->
                r != null
                    && !ObjectToolkit.isNullOrEmpty(r.getClientId())
                    && !authorizedClientIds.contains(r.getClientId().trim().toUpperCase()))
        .forEach(merged::add);

    // Aggiunge i ruoli richiesti sui client autorizzati
    safeRequested.stream()
        .filter(
            r ->
                r != null
                    && !ObjectToolkit.isNullOrEmpty(r.getClientId())
                    && authorizedClientIds.contains(r.getClientId().trim().toUpperCase()))
        .forEach(merged::add);

    return merged;
  }

  /**
   * Costruisce una mappa di lookup in memoria O(1) indicizzata per la chiave composita
   * "CLIENT_ID:ROLE".
   *
   * @param roleDefinitions Lista di definizioni dei ruoli a catalogo.
   * @return Mappa chiave-valore normalizzata in maiuscolo.
   */
  public static Map<String, RoleDefinition> buildRoleDefinitionMap(
      List<RoleDefinition> roleDefinitions) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) return Map.of();
    return roleDefinitions.stream()
        .filter(
            rd ->
                !ObjectToolkit.isNullOrEmpty(rd.getClientId())
                    && !ObjectToolkit.isNullOrEmpty(rd.getRole()))
        .collect(
            Collectors.toMap(
                rd -> buildRoleKey(rd.getClientId(), rd.getRole()),
                rd -> rd,
                (existing, replacement) -> existing,
                HashMap::new));
  }

  /**
   * Valida puntualmente ed arricchisce in memoria una lista di ruoli contro le definizioni fornite.
   *
   * @param userClientRoles Lista di ruoli forniti in input.
   * @param userIdentifier UUID univoco dell'utente associato.
   * @param roleDefinitions Lista di definizioni da verificare.
   * @return Lista di ruoli arricchiti con tipologia, descrizione e permessi.
   */
  public static List<UserClientRole> validateAndEnrichRoles(
      List<UserClientRole> userClientRoles,
      String userIdentifier,
      List<RoleDefinition> roleDefinitions) {
    if (ObjectToolkit.isNullOrEmpty(userClientRoles)) return List.of();

    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) {
      LOG.error("❌ {} No role definitions provided for validation and enrichment", ROLE_UTILS);
      throw new OAuthException(
          ExceptionMap.ERR_USER_400,
          ExceptionType.INVALID_ROLES,
          "Role definitions are required to validate and enrich roles!");
    }

    Set<String> validClientIds = extractAuthorizedClientIds(roleDefinitions);
    Map<String, RoleDefinition> roleDefMap = buildRoleDefinitionMap(roleDefinitions);

    userClientRoles.forEach(
        ucr -> {
          String clientId =
              !ObjectToolkit.isNullOrEmpty(ucr.getClientId())
                  ? ucr.getClientId().trim().toUpperCase()
                  : "";
          String role =
              !ObjectToolkit.isNullOrEmpty(ucr.getRole()) ? ucr.getRole().trim().toUpperCase() : "";

          if (!validClientIds.contains(clientId)) {
            LOG.error(
                "❌ {} Validation failed: Client ID [{}] does not exist in RoleDefinitions",
                ROLE_UTILS,
                ucr.getClientId());
            throw new OAuthException(
                ExceptionMap.ERR_USER_400,
                ExceptionType.INVALID_ROLES,
                "Invalid Client ID: " + ucr.getClientId() + " does not exist in role definitions!");
          }

          String key = buildRoleKey(clientId, role);
          if (!roleDefMap.containsKey(key)) {
            LOG.error(
                "❌ {} Validation failed: Role [{}] does not exist for Client ID [{}]",
                ROLE_UTILS,
                ucr.getRole(),
                ucr.getClientId());
            throw new OAuthException(
                ExceptionMap.ERR_USER_400,
                ExceptionType.INVALID_ROLES,
                "Invalid Role: "
                    + ucr.getRole()
                    + " is not configured for client "
                    + ucr.getClientId()
                    + "!");
          }
        });

    return userClientRoles.stream()
        .map(
            userClientRole -> {
              String key = buildRoleKey(userClientRole.getClientId(), userClientRole.getRole());
              RoleDefinition roleDef = roleDefMap.get(key);
              if (ObjectToolkit.isNullOrEmpty(roleDef)) return userClientRole;
              String targetIdentifier =
                  !ObjectToolkit.isNullOrEmpty(userClientRole.getUserIdentifier())
                      ? userClientRole.getUserIdentifier()
                      : userIdentifier;
              UserClientRole dto =
                  UserClientRoleMapper.toUserClientModel(roleDef, targetIdentifier);
              dto.setId(userClientRole.getId());
              return dto;
            })
        .toList();
  }
}
