package io.github.giovannilamarmora.accesssphere.roles;

import io.github.giovannilamarmora.accesssphere.exception.ExceptionMap;
import io.github.giovannilamarmora.accesssphere.exception.ExceptionType;
import io.github.giovannilamarmora.accesssphere.oAuth.OAuthException;
import io.github.giovannilamarmora.accesssphere.roles.database.UserClientRoleRepository;
import io.github.giovannilamarmora.accesssphere.roles.mapper.UserClientRoleMapper;
import io.github.giovannilamarmora.accesssphere.roles.model.RoleDefinition;
import io.github.giovannilamarmora.accesssphere.roles.model.UserClientRole;
import io.github.giovannilamarmora.utils.interceptors.LogInterceptor;
import io.github.giovannilamarmora.utils.interceptors.LogTimeTracker;
import io.github.giovannilamarmora.utils.interceptors.Logged;
import io.github.giovannilamarmora.utils.logger.LoggerFilter;
import io.github.giovannilamarmora.utils.utilities.ObjectToolkit;
import java.util.List;
import java.util.Map;
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
 * <ul>
 *   <li>Incapsula l'accesso a {@link UserClientRoleRepository} (tabella {@code USER_CLIENT_ROLE}).</li>
 *   <li>Collabora con {@link ClientRoleDomainService} per verificare la presenza dei ruoli a catalogo ed arricchirli.</li>
 *   <li>Utilizza {@link RoleUtils} per il mapping, l'indicizzazione e la validazione DRY.</li>
 *   <li>Consumato da {@code UserService}, {@code UserAccountService} e {@code RolesService} per la gestione dei ruoli degli utenti.</li>
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

  /**
   * LETTURA: Recupera ed arricchisce tutti i ruoli assegnati all'utente specificato,
   * unendo le associazioni di {@code USER_CLIENT_ROLE} con tipo, descrizione e permessi da {@code ROLE_DEFINITIONS}.
   *
   * <p>Ottimizzazione: Esegue 1 sola query per le associazioni e, se presenti, 1 query per
   * indicizzare le definizioni in memoria con lookup O(1) via {@link RoleUtils}, azzerando le query N+1.
   *
   * @param userIdentifier Identificatore univoco UUID dell'utente.
   * @return Lista di {@link UserClientRole} arricchita, o lista vuota se nessun ruolo è presente.
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

    return userRoles.stream()
        .map(
            ucr -> {
              String key = RoleUtils.buildRoleKey(ucr.getClientId(), ucr.getRole());
              RoleDefinition roleDef = roleDefMap.get(key);

              if (roleDef != null) {
                UserClientRole dto = UserClientRoleMapper.toUserClientModel(roleDef, userIdentifier);
                dto.setId(ucr.getId());
                return dto;
              } else {
                LOG.warn(
                    "⚠️ {} RoleDefinition not found for key: [{}], preserving fallback role",
                    USER_ROLE_DOMAIN,
                    key);
                return ucr;
              }
            })
        .toList();
  }

  /**
   * LETTURA PER CLIENT: Recupera ed arricchisce i ruoli associati all'utente limitatamente a uno specifico Client OAuth2.
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
   * SALVATAGGIO INIZIALE: Valida puntualmente e salva i ruoli per un nuovo utente (ad esempio durante {@code UserService.saveUser}).
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
   * AGGIORNAMENTO COMPLETO (REPLACE ATOMICO): Sostituisce atomicamente tutti i ruoli associati a un utente esistente.
   * Valida i nuovi ruoli, pulisce le associazioni precedenti per prevenire violazioni di {@code UK_USER_CLIENT_ROLE},
   * salva le nuove entità e restituisce i ruoli arricchiti.
   *
   * @param userIdentifier UUID univoco dell'utente da aggiornare.
   * @param newRoles Nuova lista di ruoli da assegnare all'utente.
   * @return Lista di {@link UserClientRole} aggiornata e persistita.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public List<UserClientRole> updateUserRoles(String userIdentifier, List<UserClientRole> newRoles) {
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

    // 1. Validazione puntuale dei ruoli ricevuti contro il catalogo definizioni
    List<RoleDefinition> definitions = clientRoleDomainService.getAllRoleDefinitions();
    List<UserClientRole> validatedRoles =
        RoleUtils.validateAndEnrichRoles(newRoles, userIdentifier, definitions);

    // 2. Pulizia atomica delle associazioni precedenti per azzerare il rischio di duplicate key
    userClientRoleRepository.deleteByIdentifier(userIdentifier);

    // 3. Persistenza della nuova lista di ruoli
    List<UserClientRole> savedRoles =
        userClientRoleRepository.saveAll(validatedRoles, userIdentifier);

    // 4. Fusione dei metadati informativi (descrizioni, permessi)
    return UserClientRoleMapper.mergeUserClientRoles(savedRoles, validatedRoles);
  }

  /**
   * AGGIORNAMENTO GRANULARE PER CLIENT: Aggiorna o sostituisce il ruolo dell'utente limitatamente a uno specifico Client OAuth2.
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
    if (ObjectToolkit.isNullOrEmpty(userIdentifier)
        || ObjectToolkit.isNullOrEmpty(clientId)
        || ObjectToolkit.isNullOrEmpty(newRole)) {
      throw new OAuthException(
          ExceptionMap.ERR_USER_400, "userIdentifier, clientId, and newRole are required!");
    }

    String normClientId = clientId.trim().toUpperCase();
    String normRole = newRole.trim().toUpperCase();

    // 1. Verifica che il ruolo sia censito nel catalogo per quel client delegando a ClientRoleDomainService
    RoleDefinition roleDef =
        clientRoleDomainService
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

    // 2. Rimuove il ruolo precedente dell'utente per questo client specifico
    userClientRoleRepository.deleteByUserIdentifierAndClientId(userIdentifier, normClientId);

    // 3. Salva la nuova associazione
    UserClientRole toSave = new UserClientRole(userIdentifier, normClientId, normRole);
    UserClientRole saved = userClientRoleRepository.save(toSave, userIdentifier);

    // 4. Arricchisce il ruolo salvato con tipo e permessi
    UserClientRole enriched = UserClientRoleMapper.toUserClientModel(roleDef, userIdentifier);
    enriched.setId(saved.getId());
    return enriched;
  }

  /**
   * CANCELLAZIONE TOTALE: Rimuove fisicamente tutte le associazioni di ruoli dell'utente (usato durante {@code deleteUser}).
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
   * CANCELLAZIONE PER CLIENT: Rimuove i ruoli associati all'utente per uno specifico client.
   *
   * @param userIdentifier UUID dell'utente.
   * @param clientId Identificatore del Client OAuth2.
   */
  @Transactional
  @LogInterceptor(type = LogTimeTracker.ActionType.SERVICE)
  public void deleteUserRoleForClient(String userIdentifier, String clientId) {
    if (ObjectToolkit.isNullOrEmpty(userIdentifier) || ObjectToolkit.isNullOrEmpty(clientId)) return;
    LOG.info(
        "🗑️ {} Deleting role for user identifier: {} and client: {}",
        USER_ROLE_DOMAIN,
        userIdentifier,
        clientId);
    userClientRoleRepository.deleteByUserIdentifierAndClientId(
        userIdentifier, clientId.trim().toUpperCase());
  }
}
