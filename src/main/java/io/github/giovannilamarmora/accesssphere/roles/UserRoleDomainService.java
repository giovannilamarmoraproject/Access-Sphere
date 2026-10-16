package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.UserClientRoleRepository;
import io.github.giovannilamarmora.accesssphere.roles.dto.UserSummaryDTO;
import io.github.giovannilamarmora.accesssphere.roles.mapper.RoleDefinitionMapper;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.accesssphere.user.database.UserRepository;
import io.github.giovannilamarmora.accesssphere.user.model.User;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servizio di dominio per il ciclo di vita, validazione, assegnazione e persistenza dei ruoli
 * utente contestualizzati per Client OAuth2 ({@link UserClientRole}).
 *
 * <p>Interconnessioni logiche:
 *
 * <ul>
 *   <li>Incapsula l'accesso a {@link UserClientRoleRepository} (tabella {@code USER_CLIENT_ROLE}).
 *   <li>Collabora con {@link ClientRoleDomainService} per verificare la presenza dei ruoli a
 *       catalogo ed arricchirli.
 *   <li>Collabora con {@link UserRepository} per risolvere i riepiloghi anagrafici degli utenti
 *       assegnati ai ruoli.
 *   <li>Consumato da {@code UserService} e {@link RolesService}.
 * </ul>
 */
@Service
@Logged
@RequiredArgsConstructor
public class UserRoleDomainService {

  private final Logger LOG = LoggerFilter.getLogger(this.getClass());
  private static final String USER_ROLE_DOMAIN = "User Role Domain";

  @Autowired private UserClientRoleRepository userClientRoleRepository;
  @Autowired private ClientRoleDomainService clientRoleDomainService;
  @Autowired private UserRepository userRepository;

  /**
   * Recupera ed arricchisce tutti i ruoli assegnati all'utente specificato, unendo le associazioni
   * di {@code USER_CLIENT_ROLE} con tipo, descrizione e permessi da {@code ROLE_DEFINITIONS}.
   *
   * @param userIdentifier Identificatore univoco UUID dell'utente.
   * @return Lista di {@link UserClientRole} arricchita, o lista vuota se nessun ruolo e presente.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> getUserRoles(String userIdentifier) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier)) {
      return List.of();
    }
    LOG.debug("🔍 {} Fetching and enriching roles for user: {}", USER_ROLE_DOMAIN, userIdentifier);

    List<UserClientRole> userRoles =
        userClientRoleRepository.findByUserIdentifier(userIdentifier).orElse(List.of());
    if (ObjectToolkit.isNullOrEmpty(userRoles)) {
      LOG.debug("ℹ️ {} No roles assigned for user: {}", USER_ROLE_DOMAIN, userIdentifier);
      return List.of();
    }

    Map<String, RoleDefinition> roleDefMap =
        RoleUtils.buildRoleDefinitionMap(clientRoleDomainService.getAllRoleDefinitions());

    List<UserClientRole> enrichedRoles =
        userRoles.stream()
            .map(
                userClientRole -> {
                  String key =
                      RoleUtils.buildRoleKey(
                          userClientRole.getClientId(), userClientRole.getRole());
                  RoleDefinition roleDef = roleDefMap.get(key);

                  if (!ObjectToolkit.isNullOrEmpty(roleDef)) {
                    UserClientRole dto =
                        UserClientRoleMapper.toUserClientModel(roleDef, userIdentifier);
                    dto.setId(userClientRole.getId());
                    return dto;
                  } else {
                    LOG.warn(
                        "⚠️ {} RoleDefinition not found for key: [{}], preserving fallback role",
                        USER_ROLE_DOMAIN,
                        key);
                    return userClientRole;
                  }
                })
            .toList();
    return UserClientRoleMapper.mergeUserClientRoles(userRoles, enrichedRoles);
  }

  /**
   * Arricchisce massivamente una lista di utenti con i rispettivi ruoli configurati.
   *
   * @param users Lista di utenti da arricchire.
   * @return La stessa lista di utenti con i ruoli valorizzati.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<User> enrichUserRoles(List<User> users) {
    if (ObjectToolkit.isNullOrEmpty(users)) {
      return users != null ? users : List.of();
    }
    LOG.debug("🔍 {} Batch enriching roles for {} users", USER_ROLE_DOMAIN, users.size());

    List<UserClientRole> allRoles = userClientRoleRepository.findAll().orElse(List.of());
    if (ObjectToolkit.isNullOrEmpty(allRoles)) {
      LOG.debug("ℹ️ {} No roles found in database for users", USER_ROLE_DOMAIN);
      return users;
    }

    List<RoleDefinition> definitions = clientRoleDomainService.getAllRoleDefinitions();
    List<UserClientRole> enrichedDefinitions =
        UserClientRoleMapper.toUserClientModels(definitions, null);
    List<UserClientRole> mergedRoles =
        UserClientRoleMapper.mergeUserClientRoles(allRoles, enrichedDefinitions);

    Map<String, List<UserClientRole>> rolesByUser =
        mergedRoles.stream()
            .filter(ucr -> !ObjectToolkit.isNullOrEmpty(ucr.getUserIdentifier()))
            .collect(Collectors.groupingBy(UserClientRole::getUserIdentifier));

    users.forEach(
        u -> {
          if (u != null && u.getIdentifier() != null) {
            u.setRoles(rolesByUser.getOrDefault(u.getIdentifier(), List.of()));
          }
        });

    return users;
  }

  /**
   * Recupera ed arricchisce i ruoli associati all'utente limitatamente a uno specifico Client
   * OAuth2.
   *
   * @param userIdentifier UUID univoco dell'utente.
   * @param clientId Identificatore univoco del Client OAuth2.
   * @return Lista di {@link UserClientRole} filtrata per il client specificato.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> getUserRolesForClient(String userIdentifier, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier) || ObjectToolkit.isNullOrEmpty(clientId)) {
      return List.of();
    }
    return getUserRoles(userIdentifier).stream()
        .filter(r -> clientId.trim().equalsIgnoreCase(r.getClientId()))
        .toList();
  }

  /**
   * Valida puntualmente e salva i ruoli per un nuovo utente (es. durante {@code
   * UserService.saveUser}).
   *
   * @param userIdentifier UUID dell'utente creato.
   * @param roles Lista di ruoli da associare.
   * @return Lista di ruoli arricchita e persistita.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> saveUserRoles(String userIdentifier, List<UserClientRole> roles) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier)) {
      LOG.error("❌ {} Cannot save roles: userIdentifier is missing", USER_ROLE_DOMAIN);
      throw new OAuthException(ExceptionMap.ERR_USER_400, "User identifier is required!");
    }
    if (ObjectToolkit.isNullOrEmpty(roles)) {
      LOG.warn("⚠️ {} No roles provided to save for user: {}", USER_ROLE_DOMAIN, userIdentifier);
      return List.of();
    }

    List<RoleDefinition> definitions = clientRoleDomainService.getAllRoleDefinitions();
    List<UserClientRole> validatedRoles =
        RoleUtils.validateAndEnrichRoles(roles, userIdentifier, definitions);

    List<UserClientRole> savedRoles =
        userClientRoleRepository.saveAll(validatedRoles, userIdentifier);
    return UserClientRoleMapper.mergeUserClientRoles(savedRoles, validatedRoles);
  }

  /**
   * Sostituisce atomicamente tutti i ruoli associati a un utente esistente previa validazione
   * contro il catalogo.
   *
   * @param userIdentifier UUID univoco dell'utente da aggiornare.
   * @param newRoles Nuova lista di ruoli da assegnare all'utente.
   * @return Lista di {@link UserClientRole} aggiornata e persistita.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> updateUserRoles(
      String userIdentifier, List<UserClientRole> newRoles) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier)) {
      LOG.error("❌ {} Cannot update roles: userIdentifier is missing", USER_ROLE_DOMAIN);
      throw new OAuthException(ExceptionMap.ERR_USER_400, "User identifier is required!");
    }

    if (ObjectToolkit.isNullOrEmpty(newRoles)) {
      LOG.info(
          "🗑️ {} Empty role list provided, clearing all roles for user: {}",
          USER_ROLE_DOMAIN,
          userIdentifier);
      userClientRoleRepository.deleteByIdentifier(userIdentifier);
      return List.of();
    }

    LOG.info("🔄 {} Updating roles atomically for user: {}", USER_ROLE_DOMAIN, userIdentifier);

    List<RoleDefinition> definitions = clientRoleDomainService.getAllRoleDefinitions();
    List<UserClientRole> validatedRoles =
        RoleUtils.validateAndEnrichRoles(newRoles, userIdentifier, definitions);

    userClientRoleRepository.deleteByIdentifier(userIdentifier);

    List<UserClientRole> savedRoles =
        userClientRoleRepository.saveAll(validatedRoles, userIdentifier);

    return UserClientRoleMapper.mergeUserClientRoles(savedRoles, validatedRoles);
  }

  /**
   * Aggiorna o sostituisce il ruolo dell'utente limitatamente a uno specifico Client OAuth2.
   *
   * @param userIdentifier UUID univoco dell'utente.
   * @param clientId Identificatore del Client OAuth2 target.
   * @param newRole Nome del nuovo ruolo da assegnare per il client specificato.
   * @return Modello {@link UserClientRole} aggiornato ed arricchito.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public UserClientRole updateUserRoleForClient(
      String userIdentifier, String clientId, String newRole) {
    RoleValidator.validateUserRoleParams(userIdentifier, clientId, newRole);

    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(newRole);

    RoleDefinition roleDef = verifyRoleConfiguredForClient(normClientId, normRole);

    userClientRoleRepository.deleteByUserIdentifierAndClientId(userIdentifier, normClientId);

    UserClientRole toSave = new UserClientRole(userIdentifier, normClientId, normRole);
    UserClientRole saved = userClientRoleRepository.save(toSave, userIdentifier);

    if (ObjectToolkit.isNullOrEmpty(roleDef)) return saved;
    UserClientRole enriched = UserClientRoleMapper.toUserClientModel(roleDef, userIdentifier);
    enriched.setId(saved.getId());
    return enriched;
  }

  /**
   * Assegna un singolo ruolo contestualizzato per uno specifico Client OAuth2 a un utente (senza
   * rimuovere altri ruoli gia posseduti sullo stesso client) e restituisce la lista completa
   * aggiornata dei ruoli utente.
   *
   * @param userIdentifier UUID dell'utente.
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo da assegnare.
   * @return Lista aggiornata ed arricchita dei ruoli dell'utente.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> assignUserRoleForClient(
      String userIdentifier, String clientId, String role) {
    RoleValidator.validateUserRoleParams(userIdentifier, clientId, role);

    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);

    verifyRoleConfiguredForClient(normClientId, normRole);

    if (!userClientRoleRepository.existsByUserIdentifierAndClientIdAndRole(
        userIdentifier, normClientId, normRole)) {
      UserClientRole toSave = new UserClientRole(userIdentifier, normClientId, normRole);
      userClientRoleRepository.save(toSave, userIdentifier);
      LOG.info(
          "💾 {} Assigned role [{}] on client [{}] to user [{}]",
          USER_ROLE_DOMAIN,
          normRole,
          normClientId,
          userIdentifier);
    }

    return getUserRoles(userIdentifier);
  }

  /**
   * Revoca un singolo ruolo contestualizzato per uno specifico Client OAuth2 da un utente e
   * restituisce la lista aggiornata dei ruoli rimanenti.
   *
   * @param userIdentifier UUID dell'utente.
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo da revocare.
   * @return Lista aggiornata ed arricchita dei ruoli dell'utente.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> revokeUserRoleForClient(
      String userIdentifier, String clientId, String role) {
    RoleValidator.validateUserRoleParams(userIdentifier, clientId, role);

    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);

    userClientRoleRepository.deleteByUserIdentifierAndClientIdAndRole(
        userIdentifier, normClientId, normRole);
    LOG.info(
        "🗑️ {} Revoked role [{}] on client [{}] from user [{}]",
        USER_ROLE_DOMAIN,
        normRole,
        normClientId,
        userIdentifier);

    return getUserRoles(userIdentifier);
  }

  /**
   * Recupera la lista sintetizzata degli utenti a cui e attualmente assegnato uno specifico ruolo
   * per un determinato Client OAuth2.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo.
   * @return Lista di {@link UserSummaryDTO}.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserSummaryDTO> getUsersAssignedToRole(String clientId, String role) {
    if (ObjectToolkit.isNullOrEmpty(clientId) || ObjectToolkit.isNullOrEmpty(role)) {
      return List.of();
    }
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);

    List<UserClientRole> assignments =
        userClientRoleRepository.findByClientIdAndRole(normClientId, normRole).orElse(List.of());
    List<String> userIdentifiers =
        assignments.stream()
            .map(UserClientRole::getUserIdentifier)
            .filter(id -> !ObjectToolkit.isNullOrEmpty(id))
            .distinct()
            .toList();

    if (userIdentifiers.isEmpty()) {
      return List.of();
    }

    List<User> users = userRepository.findAllByIdentifierIn(userIdentifiers).orElse(List.of());
    return RoleDefinitionMapper.toUserSummaryList(users);
  }

  /**
   * Arricchisce una lista di definizioni di ruolo con il conteggio e il riepilogo degli utenti
   * assegnati.
   *
   * @param roleDefinitions Lista di {@link RoleDefinition} da arricchire.
   * @return Lista di {@link RoleDefinition} arricchita.
   */
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<RoleDefinition> enrichRoleDefinitionsWithAssignedUsers(
      List<RoleDefinition> roleDefinitions) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinitions)) return List.of();
    roleDefinitions.forEach(this::enrichRoleDefinitionWithAssignedUsers);
    return roleDefinitions;
  }

  /**
   * Arricchisce una singola definizione di ruolo con il conteggio e il riepilogo degli utenti
   * assegnati.
   *
   * @param roleDefinition Definizione di ruolo da arricchire.
   * @return La stessa {@link RoleDefinition} arricchita.
   */
  public RoleDefinition enrichRoleDefinitionWithAssignedUsers(RoleDefinition roleDefinition) {
    if (ObjectToolkit.isNullOrEmpty(roleDefinition)) return roleDefinition;
    List<UserSummaryDTO> assignedUsers =
        getUsersAssignedToRole(roleDefinition.getClientId(), roleDefinition.getRole());
    roleDefinition.setAssignedUsers(assignedUsers);
    roleDefinition.setAssignedUsersCount(assignedUsers.size());
    return roleDefinition;
  }

  /**
   * Aggiorna a cascata tutte le associazioni utente in {@code USER_CLIENT_ROLE} quando un ruolo
   * viene rinominato a catalogo.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param oldRoleName Nome precedente del ruolo.
   * @param newRoleName Nuovo nome del ruolo.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void cascadeRenameRoleAssignments(
      String clientId, String oldRoleName, String newRoleName) {
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normOldRole = RoleUtils.normalizeUpperCase(oldRoleName);
    String normNewRole = RoleUtils.normalizeUpperCase(newRoleName);

    if (ObjectToolkit.isNullOrEmpty(normClientId)
        || ObjectToolkit.isNullOrEmpty(normOldRole)
        || ObjectToolkit.isNullOrEmpty(normNewRole)
        || normOldRole.equalsIgnoreCase(normNewRole)) {
      return;
    }

    List<UserClientRole> existingMappings =
        userClientRoleRepository.findByClientIdAndRole(normClientId, normOldRole).orElse(List.of());
    existingMappings.forEach(
        mapping -> {
          mapping.setRole(normNewRole);
          userClientRoleRepository.save(mapping, mapping.getUserIdentifier());
        });
    LOG.info(
        "🔄 {} Cascade renamed {} assignments from [{}] to [{}] on client [{}]",
        USER_ROLE_DOMAIN,
        existingMappings.size(),
        normOldRole,
        normNewRole,
        normClientId);
  }

  /**
   * Rimuove a cascata tutte le assegnazioni utente per un ruolo eliminato dal catalogo e
   * restituisce il numero di utenti distinti impattati.
   *
   * @param clientId Identificativo del Client OAuth2.
   * @param role Nome del ruolo eliminato.
   * @return Numero di utenti distinti da cui il ruolo e stato revocato.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public int cascadeDeleteRoleAssignments(String clientId, String role) {
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    String normRole = RoleUtils.normalizeUpperCase(role);

    List<UserClientRole> impactedAssignments =
        userClientRoleRepository.findByClientIdAndRole(normClientId, normRole).orElse(List.of());
    int impactedUsersCount =
        (int)
            impactedAssignments.stream()
                .map(UserClientRole::getUserIdentifier)
                .filter(id -> !ObjectToolkit.isNullOrEmpty(id))
                .distinct()
                .count();

    userClientRoleRepository.deleteByClientIdAndRole(normClientId, normRole);
    LOG.info(
        "🗑️ {} Cascade deleted {} assignments ({} distinct users) for role [{}] on client [{}]",
        USER_ROLE_DOMAIN,
        impactedAssignments.size(),
        impactedUsersCount,
        normRole,
        normClientId);
    return impactedUsersCount;
  }

  /**
   * Rimuove fisicamente tutte le associazioni di ruoli dell'utente (usato durante {@code
   * deleteUser}).
   *
   * @param userIdentifier UUID dell'utente.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserRoles(String userIdentifier) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier)) return;
    LOG.info("🗑️ {} Deleting all roles for user identifier: {}", USER_ROLE_DOMAIN, userIdentifier);
    userClientRoleRepository.deleteByIdentifier(userIdentifier);
  }

  /**
   * Rimuove i ruoli associati all'utente per uno specifico client.
   *
   * @param userIdentifier UUID dell'utente.
   * @param clientId Identificatore del Client OAuth2.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserRoleForClient(String userIdentifier, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier) || ObjectToolkit.isNullOrEmpty(clientId))
      return;
    LOG.info(
        "🗑️ {} Deleting role for user identifier: {} and client: {}",
        USER_ROLE_DOMAIN,
        userIdentifier,
        clientId);
    userClientRoleRepository.deleteByUserIdentifierAndClientId(
        userIdentifier, RoleUtils.normalizeUpperCase(clientId));
  }

  /**
   * Rimuove a cascata tutte le associazioni di ruoli utente collegate a uno specifico Client OAuth2
   * (utilizzato durante la cancellazione del client in {@code OAuth2ClientDomainService}).
   *
   * @param clientId Identificatore univoco del Client OAuth2.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserRolesByClientId(String clientId) {
    if (ObjectToolkit.isNullOrEmpty(clientId)) return;
    String normClientId = RoleUtils.normalizeUpperCase(clientId);
    LOG.info(
        "🗑️ {} Deleting all user role assignments for client: {}", USER_ROLE_DOMAIN, normClientId);
    userClientRoleRepository.deleteByClientId(normClientId);
  }

  private RoleDefinition verifyRoleConfiguredForClient(String normClientId, String normRole) {
    return clientRoleDomainService
        .getRoleDefinition(normClientId, normRole)
        .orElseThrow(
            () -> {
              LOG.error(
                  "❌ {} Role [{}] is not configured for client [{}]",
                  USER_ROLE_DOMAIN,
                  normRole,
                  normClientId);
              return new OAuthException(
                  ExceptionMap.ERR_USER_400,
                  ExceptionType.INVALID_ROLES,
                  "Role " + normRole + " is not configured for client " + normClientId);
            });
  }
}
