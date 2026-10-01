package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;

/**
 * Utility di dominio condivisa per le operazioni sui ruoli (normalizzazione chiavi, indicizzazione
 * in memoria, validazione contro catalogo ed arricchimento DTO).
 *
 * <p>Interconnessioni:
 *
 * <ul>
 *   <li>Consumato da {@link UserRoleDomainService} per validare i ruoli utente in ingresso ed
 *       arricchirli con permessi.
 *   <li>Consumato da {@link ClientRoleDomainService} e da altri componenti di dominio per il
 *       calcolo univoco delle chiavi composita "CLIENT_ID:ROLE".
 * </ul>
 */
public final class RoleUtils {

  private static final Logger LOG = LoggerFilter.getLogger(RoleUtils.class);
  private static final String ROLE_UTILS = "Role Utils";

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

    // Set di Client ID validi tramite Stream API
    Set<String> validClientIds =
        roleDefinitions.stream()
            .map(RoleDefinition::getClientId)
            .filter(cid -> !ObjectToolkit.isNullOrEmpty(cid))
            .map(cid -> cid.trim().toUpperCase())
            .collect(Collectors.toSet());

    // Mappa di lookup O(1) per CLIENT_ID:ROLE riutilizzando buildRoleDefinitionMap
    Map<String, RoleDefinition> roleDefMap = buildRoleDefinitionMap(roleDefinitions);

    // Validazione puntuale di esistenza Client e Ruolo tramite forEach
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

    // Mappatura ed arricchimento garantiti tramite Stream API
    return userClientRoles.stream()
        .map(
            userClientRole -> {
              String key = buildRoleKey(userClientRole.getClientId(), userClientRole.getRole());
              RoleDefinition roleDef = roleDefMap.get(key);
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
